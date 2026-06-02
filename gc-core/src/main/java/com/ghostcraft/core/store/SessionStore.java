package com.ghostcraft.core.store;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore {

    private static final Logger log = (Logger) LoggerFactory.getLogger(SessionStore.class);
    private static final Path STORE_DIR = Paths.get(System.getProperty("user.home"), ".ghostcraft", "sessions");
    private final ObjectMapper mapper = new ObjectMapper();

    /** 内存中缓存的摘要：sessionId → 摘要文本 */
    private final Map<String, String> summaries = new ConcurrentHashMap<>();

    public SessionStore() {
        try {
            Files.createDirectories(STORE_DIR);
            log.info("会话存储目录: {}", STORE_DIR.toAbsolutePath());
        } catch (IOException e) {
            log.warn("创建存储目录失败: {}", e.getMessage());
        }
    }

    /** 保存会话的摘要 */
    public void saveSummary(String sessionId, String sessionName, String summary) {
        summaries.put(sessionId, summary);
        File file = STORE_DIR.resolve(sessionId + ".json").toFile();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file,
                    Map.of("id", sessionId, "name", sessionName, "summary", summary,
                            "time", System.currentTimeMillis()));
        } catch (IOException e) {
            log.warn("保存会话 {} 失败: {}", sessionId, e.getMessage());
        }
    }

    /** 加载所有持久化的会话摘要 */
    public void loadAll() {
        File[] files = STORE_DIR.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            try {
                Map<String, Object> data = mapper.readValue(file,
                        new TypeReference<Map<String, Object>>() {});
                String id = (String) data.get("id");
                String summary = (String) data.get("summary");
                if (id != null && summary != null) {
                    summaries.put(id, summary);
                }
            } catch (IOException e) {
                log.warn("加载文件 {} 失败: {}", file.getName(), e.getMessage());
            }
        }
        log.info("已加载 {} 个持久化会话", summaries.size());
    }

    /** 获取指定会话的摘要 */
    public String getSummary(String sessionId) {
        return summaries.get(sessionId);
    }

    /** 搜索所有持久化会话（跨会话记忆） */
    public String search(String keyword) {
        StringBuilder sb = new StringBuilder("跨会话搜索结果（关键词: " + keyword + "）：\n");
        for (Map.Entry<String, String> entry : summaries.entrySet()) {
            if (entry.getValue().contains(keyword)) {
                sb.append("  [").append(entry.getKey()).append("] ").append(entry.getValue()).append("\n");
            }
        }
        if (sb.toString().contains("搜索结果") && sb.length() < 30) {
            sb.append("  无匹配结果\n");
        }
        return sb.toString();
    }

    /** 获取所有持久化会话的 ID 和名称 */
    public Map<String, String> getAllSessions() {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        File[] files = STORE_DIR.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return result;
        for (File file : files) {
            try {
                Map<String, Object> data = mapper.readValue(file,
                        new TypeReference<Map<String, Object>>() {});
                String id = (String) data.get("id");
                String name = (String) data.get("name");
                if (id != null && name != null) {
                    result.put(id, name);
                }
            } catch (IOException e) {
                log.warn("读取文件 {} 失败: {}", file.getName(), e.getMessage());
            }
        }
        return result;
    }

    /** 列出所有持久化会话 */
    public String listAll() {
        if (summaries.isEmpty()) return "暂无持久化会话";
        StringBuilder sb = new StringBuilder("持久化会话：\n");
        for (var entry : summaries.entrySet()) {
            String summary = entry.getValue();
            String firstLine = summary.contains("\n") ? summary.substring(0, summary.indexOf("\n")) : summary;
            sb.append("  [").append(entry.getKey()).append("] ").append(firstLine).append("\n");
        }
        return sb.toString();
    }
}
