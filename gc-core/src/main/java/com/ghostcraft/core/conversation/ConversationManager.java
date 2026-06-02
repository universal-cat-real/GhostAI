package com.ghostcraft.core.conversation;

import com.ghostcraft.core.hook.HookRegistry;
import com.ghostcraft.core.model.Session;
import com.ghostcraft.core.skill.SkillRegistry;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConversationManager {

    @Autowired
    private SkillRegistry skillRegistry;

    @Autowired
    private HookRegistry hookRegistry;

    private ChatModel model;
    private final Map<String, SessionAgent> sessions = new HashMap<>();

    @Value("${ghostcraft.api-key}")
    private String apiKey;

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

    private ChatModel getOrCreateModel() {
        if (model == null) {
            model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl("https://api.deepseek.com")
                    .modelName("deepseek-chat")
                    .temperature(0.7)
                    .build();
        }
        return model;
    }

    public SkillRegistry getSkillRegistry() {
        return skillRegistry;
    }

    public HookRegistry getHookRegistry() {
        return hookRegistry;
    }

    public String createSession(String name) {
        Session s = new Session(name);
        sessions.put(s.getId(), new SessionAgent(s, getOrCreateModel(), skillRegistry.allToolInstances()));
        return s.getId();
    }

    public String chat(String sessionId, String message) {
        SessionAgent sa = sessions.get(sessionId);
        if (sa == null) {
            return "会话不存在：" + sessionId;
        }
        hookRegistry.fireBeforeChat(sessionId, message);
        try {
            String answer = sa.chat(message);
            hookRegistry.fireAfterChat(sessionId, message, answer);
            return answer;
        } catch (Exception e) {
            hookRegistry.fireOnError(sessionId, message, e);
            return "处理出错：" + e.getMessage();
        }
    }

    public List<Session> listSessions() {
        return sessions.values().stream().map(sa -> sa.session).toList();
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
