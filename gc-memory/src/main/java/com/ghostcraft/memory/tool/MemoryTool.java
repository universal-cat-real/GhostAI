package com.ghostcraft.memory.tool;

import com.ghostcraft.core.conversation.ConversationManager;
import com.ghostcraft.core.tools.GhostTool;
import com.ghostcraft.memory.store.MemoryChunk;
import com.ghostcraft.memory.store.MemoryStore;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzh.BgeSmallZhEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemoryTool implements GhostTool {

    @Autowired
    private MemoryStore memoryStore;

    @Autowired
    private ConversationManager conversationManager;

    @Autowired
    private dev.langchain4j.model.chat.ChatModel chatModel;

    private final EmbeddingModel embeddingModel = new BgeSmallZhEmbeddingModel();

    @Tool("保存一段对话到长期记忆，传入完整的对话内容，系统会自动生成向量和关键词")
    public String saveMemory(@P("要保存的对话内容") String content) {
        String sessionId = conversationManager.getRootAgentId();
        MemoryChunk chunk = new MemoryChunk(sessionId, content);
        chunk.setEmbedding(embeddingModel.embed(content).content().vector());
        chunk.setKeywords(extractKeywords(content));
        memoryStore.save(chunk);
        return "记忆已保存";
    }

    @Tool("搜索历史记忆，传入关键词，返回语义匹配和关键词匹配混合排序的结果")
    public String searchMemory(@P("搜索关键词") String query) {
        String sessionId = conversationManager.getRootAgentId();
        var results = memoryStore.search(sessionId, query);
        if (results.isEmpty()) return "未找到相关记忆";
        StringBuilder sb = new StringBuilder("找到 " + results.size() + " 条相关记忆：\n\n");
        for (MemoryChunk c : results) {
            sb.append("[得分 ").append(String.format("%.2f", c.score())).append("]\n");
            String preview = c.content().length() > 300
                    ? c.content().substring(0, 300) + "..."
                    : c.content();
            sb.append(preview).append("\n\n");
        }
        return sb.toString();
    }

    private String extractKeywords(String text) {
        try {
            String input = text.length() > 300 ? text.substring(0, 300) : text;
            return chatModel.chat("""
                    从以下文本中提取3-5个关键词，用逗号分隔，只输出关键词本身，不要多余内容。
                    
                    %s
                    """.formatted(input));
        } catch (Exception e) {
            String fallback = text.length() > 30 ? text.substring(0, 30) : text;
            return fallback;
        }
    }
}
