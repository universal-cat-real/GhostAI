package com.ghostcraft.memory.store;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzh.BgeSmallZhEmbeddingModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MemoryStore {

    private static final Logger log = LoggerFactory.getLogger(MemoryStore.class);
    private static final Path DB_PATH = Paths.get(
            System.getProperty("user.home"), ".ghostcraft", "memory.db");

    private static final float VECTOR_WEIGHT = 0.7f;
    private static final float KEYWORD_WEIGHT = 0.3f;
    private static final int TOP_K = 5;

    private Connection conn;
    private EmbeddingModel embeddingModel;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(DB_PATH.getParent());
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH.toAbsolutePath());
            createTable();
            embeddingModel = new BgeSmallZhEmbeddingModel();
            log.info("MemoryStore 初始化完成, DB: {}", DB_PATH.toAbsolutePath());
        } catch (Exception e) {
            log.warn("MemoryStore 初始化失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        try { if (conn != null) conn.close(); } catch (Exception e) {}
    }

    private void createTable() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS chunks (
                    id TEXT PRIMARY KEY,
                    session_id TEXT NOT NULL,
                    content TEXT NOT NULL,
                    keywords TEXT DEFAULT '',
                    embedding BLOB,
                    created_at TEXT DEFAULT (datetime('now'))
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_chunks_session ON chunks(session_id)");
        }
    }

    /**
     * 保存索引到sqlite
     * @param chunk
     */
    public void save(MemoryChunk chunk) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO chunks (id, session_id, content, keywords, embedding) VALUES (?,?,?,?,?)")) {
            ps.setString(1, chunk.id());
            ps.setString(2, chunk.sessionId());
            ps.setString(3, chunk.content());
            ps.setString(4, chunk.keywords() != null ? chunk.keywords() : "");
            if (chunk.embedding() != null) {
                ps.setBytes(5, floatsToBytes(chunk.embedding()));
            } else {
                ps.setNull(5, Types.BLOB);
            }
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("保存记忆失败: {}", e.getMessage());
        }
    }

    /**
     * 查询
     * @param sessionId
     * @param query
     * @return
     */
    public List<MemoryChunk> search(String sessionId, String query) {
        try {
            // 1. 计算查询向量的 embedding
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            float[] queryVec = queryEmbedding.vector();

            // 2. 从 SQLite 读出当前会话的所有 chunk
            List<MemoryChunk> all = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, session_id, content, keywords, embedding FROM chunks WHERE session_id = ?")) {
                ps.setString(1, sessionId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    MemoryChunk c = new MemoryChunk(rs.getString("session_id"), rs.getString("content"));
                    // 通过反射注入 id（简化处理）
                    byte[] embBytes = rs.getBytes("embedding");
                    if (embBytes != null) {
                        float[] emb = bytesToFloats(embBytes);
                        c.setEmbedding(emb);
                    }
                    c.setKeywords(rs.getString("keywords"));

                    // 计算混合得分
                    double vectorScore = 0;
                    if (c.embedding() != null && queryVec != null) {
                        vectorScore = cosineSimilarity(c.embedding(), queryVec);
                    }
                    double keywordScore = keywordMatch(c.content(), c.keywords(), query);
                    c.setScore(vectorScore * VECTOR_WEIGHT + keywordScore * KEYWORD_WEIGHT);
                    all.add(c);
                }
            }

            // 3. 按得分排序，取 TOP_K
            all.sort((a, b) -> Double.compare(b.score(), a.score()));
            return all.stream().limit(TOP_K).collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("搜索记忆失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取相关记忆信息
     * @param sessionId
     * @param query
     * @return
     */
    public String getRelevantContext(String sessionId, String query) {
        List<MemoryChunk> chunks = search(sessionId, query);
        if (chunks.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("\n\n## 相关历史记忆\n");
        for (MemoryChunk c : chunks) {
            sb.append("- [分 ").append(String.format("%.2f", c.score())).append("] ");
            String preview = c.content().length() > 200
                    ? c.content().substring(0, 200) + "..."
                    : c.content();
            sb.append(preview).append("\n");
        }
        return sb.toString();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    /**
     * 关键词检索
     * @param content
     * @param keywords
     * @param query
     * @return
     */
    private double keywordMatch(String content, String keywords, String query) {
        String q = query.toLowerCase();
        double score = 0;
        if (content != null && content.toLowerCase().contains(q)) score += 0.5;
        if (keywords != null && keywords.toLowerCase().contains(q)) score += 0.5;
        return Math.min(score, 1.0);
    }

    private byte[] floatsToBytes(float[] floats) {
        ByteBuffer buf = ByteBuffer.allocate(floats.length * 4);
        for (float f : floats) buf.putFloat(f);
        return buf.array();
    }

    private float[] bytesToFloats(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) floats[i] = buf.getFloat();
        return floats;
    }
}
