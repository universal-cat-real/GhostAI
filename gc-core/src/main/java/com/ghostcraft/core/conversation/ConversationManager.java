package com.ghostcraft.core.conversation;

import com.ghostcraft.core.model.Session;
import com.ghostcraft.core.skill.SkillRegistry;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话管理器
 * 每个会话有独立的 agent + memory
 * 每个会话互不干扰
 */
public class ConversationManager {

    private final ChatModel model;
    private final SkillRegistry skillRegistry;
    private final Map<String, SessionAgent> sessions;

    public ConversationManager(String appkey) {
        this.model = OpenAiChatModel.builder()
                .apiKey(appkey)
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-chat")
                .temperature(0.7)
                .build();
        this.sessions = new HashMap<>();
        this.skillRegistry = new SkillRegistry();
    }

    public interface Assistant {
        String chat(String userMessage);
    }

    private static class SessionAgent {
        final Session session;
        final Assistant agent;
        final ChatMemory memory;

        SessionAgent(Session session, ChatModel model, List<Object> tools) {
            this.session = session;
            this.memory = MessageWindowChatMemory.builder()
                    .maxMessages(20)
                    .build();

            var builder = AiServices.builder(Assistant.class)
                    .chatModel(model)
                    .chatMemory(memory);

            if (tools != null && !tools.isEmpty()) {
                builder.tools(tools.toArray());
            }

            this.agent = builder.build();
        }

        String chat(String message) {
            session.touch();
            return agent.chat(message);
        }
    }

    public SkillRegistry getSkillRegistry() {
        return skillRegistry;
    }

    public String createSession(String name) {
        Session s = new Session(name);
        sessions.put(s.getId(), new SessionAgent(s, model, skillRegistry.allToolInstances()));
        return s.getId();
    }

    public String chat(String sessionId, String message) {
        SessionAgent sa = sessions.get(sessionId);
        if (sa == null) {
            return "会话不存在：" + sessionId;
        }
        return sa.chat(message);
    }

    public List<Session> listSessions() {
        return sessions.values().stream()
                .map(sa -> sa.session)
                .toList();
    }

    public void clearMemory(String sessionId) {
        SessionAgent sa = sessions.get(sessionId);
        if (sa != null) {
            sa.memory.clear();
        }
    }

    public int sessionCount() {
        return sessions.size();
    }
}
