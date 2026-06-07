package com.ghostcraft.memory.store;

import com.ghostcraft.core.conversation.ConversationManager;
import com.ghostcraft.core.hook.Hook;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzh.BgeSmallZhEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryPersistHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(MemoryPersistHook.class);

    private final MemoryStore memoryStore;
    private final ChatModel chatModel;
    private final ConversationManager conversationManager;
    private final EmbeddingModel embeddingModel = new BgeSmallZhEmbeddingModel();

    public MemoryPersistHook(MemoryStore memoryStore, ChatModel chatModel, ConversationManager conversationManager) {
        this.memoryStore = memoryStore;
        this.chatModel = chatModel;
        this.conversationManager = conversationManager;
    }

    @Override
    public void afterChat(String sessionId, String userMessage, String aiMessage) {
        if (userMessage == null || userMessage.isBlank()) return;
        try {
            var msgs = conversationManager.getSessionMessages(sessionId);
            StringBuilder turn = new StringBuilder();
            for (int i = msgs.size() - 1; i >= 0; i--) {
                var msg = msgs.get(i);
                if (msg instanceof SystemMessage) continue;
                String prefix;
                String content;
                if (msg instanceof UserMessage um) {
                    prefix = "用户: ";
                    content = um.singleText();
                } else if (msg instanceof AiMessage am) {
                    if (am.text() != null) {
                        prefix = "AI: ";
                        content = am.text();
                    } else if (am.hasToolExecutionRequests()) {
                        // AI 消息携带工具调用请求
                        StringBuilder sb = new StringBuilder("调用工具:\n");
                        for (ToolExecutionRequest req : am.toolExecutionRequests()) {
                            sb.append("  - ").append(req.name()).append("(").append(req.arguments()).append(")\n");
                        }
                        prefix = "";
                        content = sb.toString().trim();
                    } else {
                        prefix = "AI: ";
                        content = "(空)";
                    }
                } else if (msg instanceof ToolExecutionResultMessage trm) {
                    prefix = "工具结果[" + trm.toolName() + "]: ";
                    content = trm.text() != null ? trm.text() : "(空)";
                } else {
                    prefix = "其他: ";
                    content = msg.toString();
                }
                turn.insert(0, prefix + content + "\n");
                if (msg instanceof UserMessage) break;
            }
            String full = turn.toString().trim();
            if (full.isBlank()) return;

            MemoryChunk chunk = new MemoryChunk(sessionId, full);
            chunk.setEmbedding(embeddingModel.embed(full).content().vector());
            chunk.setKeywords(extractKeywords(full));
            memoryStore.save(chunk);
        } catch (Exception e) {
            log.warn("自动持久化记忆失败: {}", e.getMessage());
        }
    }

    private String extractKeywords(String text) {
        try {
            String input = text.length() > 300 ? text.substring(0, 300) : text;
            return chatModel.chat("""
                    从以下对话中提取3-5个关键词，用逗号分隔，只输出关键词本身。
                    %s
                    """.formatted(input));
        } catch (Exception e) {
            return text.length() > 30 ? text.substring(0, 30) : text;
        }
    }

    @Override
    public String name() { return "MemoryPersistHook"; }
}
