package com.ghostcraft.core.conversation;

import com.ghostcraft.core.hook.HookRegistry;
import com.ghostcraft.core.hook.PersistHook;
import com.ghostcraft.core.hook.TokenCounterHook;
import com.ghostcraft.core.skill.Skill;
import com.ghostcraft.core.model.Session;
import com.ghostcraft.core.skill.SkillRegistry;
import com.ghostcraft.core.tools.SessionTool;
import com.ghostcraft.core.tools.SubTaskTool;
import com.ghostcraft.core.store.SessionStore;
import com.ghostcraft.core.subtask.SubTaskManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConversationManager {

    private static final Logger log = LoggerFactory.getLogger(ConversationManager.class);
    private static final int MAX_VERBATIM = 6;

    @Autowired
    private SkillRegistry skillRegistry;

    @Autowired
    private HookRegistry hookRegistry;

    @Autowired
    private TokenCounterHook tokenCounter;

    @Autowired
    private SessionStore sessionStore;

    @Autowired
    private SessionTool sessionTool;

    @Autowired
    private SubTaskTool subTaskTool;

    @Autowired
    private SubTaskManager subTaskManager;

    private ChatModel model;
    private final Map<String, SessionAgent> sessions = new HashMap<>();
    private String activeSessionId;

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
            this.memory = MessageWindowChatMemory.builder().maxMessages(20).build();
            var builder = AiServices.builder(Assistant.class)
                    .chatModel(model).chatMemory(memory)
                    .maxToolCallingRoundTrips(20);   // ← 加这一行
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

    public String getActiveSessionId() { return activeSessionId; }

    public void setActiveSessionId(String sessionId) {
        this.activeSessionId = sessionId;
        log.info("当前会话切换到: {}", sessionId);
    }

    @PostConstruct
    public void init() {
        hookRegistry.register(tokenCounter);
        hookRegistry.register(new PersistHook(this));
        skillRegistry.register(new Skill() {
            public String name() { return "session"; }
            public String description() { return "会话管理：列出、创建、切换会话"; }
            public Object toolInstance() { return sessionTool; }
        });
        skillRegistry.register(new Skill() {
            public String name() { return "subtask"; }
            public String description() { return "子任务：创建、查询异步子任务"; }
            public Object toolInstance() { return subTaskTool; }
        });
        sessionStore.loadAll();
        // 从持久化文件恢复会话
        var stored = sessionStore.getAllSessions();
        for (var entry : stored.entrySet()) {
            String id = entry.getKey();
            String name = entry.getValue();
            if (!sessions.containsKey(id)) {
                Session s = new Session(id, name);
                sessions.put(id, new SessionAgent(s, getOrCreateModel(), skillRegistry.allToolInstances()));
            }
        }
        log.info("ConversationManager 初始化完成, 共 {} 个活跃会话", sessions.size());
    }

    private ChatModel getOrCreateModel() {
        if (model == null) {
            model = OpenAiChatModel.builder()
                    .apiKey(apiKey).baseUrl("https://api.deepseek.com")
                    .modelName("deepseek-chat").temperature(0.7)
                    .build();
        }
        return model;
    }

    public SkillRegistry getSkillRegistry() { return skillRegistry; }
    public HookRegistry getHookRegistry() { return hookRegistry; }

    public String createSession(String name) {
        Session s = new Session(name);
        sessions.put(s.getId(), new SessionAgent(s, getOrCreateModel(), skillRegistry.allToolInstances()));
        sessionStore.saveSummary(s.getId(), name, "新会话: " + name);
        setActiveSessionId(s.getId());
        return s.getId();
    }

    public String chat(String sessionId, String message) {
        SessionAgent sa = sessions.get(sessionId);
        if (sa == null) return "会话不存在: " + sessionId;

        // 检查是否有子任务完成，注入结果到记忆
        var completed = subTaskManager.pollCompletion(sessionId);
        if (completed != null) {
            sa.memory.add(new dev.langchain4j.data.message.SystemMessage(
                    "子任务 [" + completed.taskId() + "] " + completed.description() + " 已完成，结果如下:\n" + completed.result()));
        }

        hookRegistry.fireBeforeChat(sessionId, message);
        try {
            String answer = sa.chat(message);
            hookRegistry.fireAfterChat(sessionId, message, answer);
            compressOrSlide(sessionId);
            return answer;
        } catch (Exception e) {
            hookRegistry.fireOnError(sessionId, message, e);
            return "处理出错: " + e.getMessage();
        }
    }

    public List<Session> listSessions() {
        return sessions.values().stream().map(sa -> sa.session).toList();
    }

    public void clearMemory(String sessionId) {
        SessionAgent sa = sessions.get(sessionId);
        if (sa != null) sa.memory.clear();
    }

    public int sessionCount() { return sessions.size(); }

    public String getMemoryStats() {
        StringBuilder sb = new StringBuilder("记忆状态:\n");
        sb.append("总会话数: ").append(sessions.size()).append("\n");
        for (Map.Entry<String, SessionAgent> entry : sessions.entrySet()) {
            String sid = entry.getKey();
            SessionAgent sa = entry.getValue();
            sb.append("  [").append(sid).append("] ").append(sa.session.getName())
                    .append(" -- 消息: ").append(sa.memory.messages().size()).append(" 条")
                    .append(" | ").append(tokenCounter.getStats(sid)).append("\n");
        }
        return sb.toString();
    }

    public String crossSessionQuery(String keyword) {
        return sessionStore.search(keyword);
    }

    public String listStoredSessions() {
        return sessionStore.listAll();
    }

    public void persistIfDue(String sessionId) {
        String stats = tokenCounter.getStats(sessionId);
        if (!stats.contains("消息: ")) return;
        try {
            String msgPart = stats.split("消息: ")[1].split(" \\|")[0].trim();
            int msgCount = Integer.parseInt(msgPart);
            if (msgCount > 0 && msgCount % 5 == 0) {
                SessionAgent sa = sessions.get(sessionId);
                if (sa == null) return;
                String summary = "";
                for (ChatMessage msg : sa.memory.messages()) {
                    if (msg instanceof SystemMessage sm
                            && sm.text() != null && sm.text().startsWith("历史摘要")) {
                        summary = sm.text().replace("历史摘要：", "");
                    }
                }
                if (!summary.isEmpty()) {
                    sessionStore.saveSummary(sessionId, sa.session.getName(), summary);
                    log.info("会话 {} 第 {} 轮, 已自动持久化", sessionId, msgCount);
                }
            }
        } catch (Exception e) {
            log.warn("持久化检查失败: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════ 压缩 ═══════════════════════════════

    public void compressOrSlide(String sessionId) {
        SessionAgent sa = sessions.get(sessionId);
        if (sa == null) return;

        List<ChatMessage> all = sa.memory.messages();
        List<ChatMessage> rawMessages = all.stream()
                .filter(m -> !(m instanceof SystemMessage)).toList();

        if (rawMessages.size() <= MAX_VERBATIM) return;

        int compressCount = rawMessages.size() - MAX_VERBATIM;
        List<ChatMessage> toCompress = rawMessages.subList(0, compressCount);
        List<ChatMessage> keep = rawMessages.subList(compressCount, rawMessages.size());

        String existingSummary = "";
        for (ChatMessage msg : all) {
            if (msg instanceof SystemMessage sm && sm.text() != null
                    && sm.text().startsWith("历史摘要")) {
                existingSummary = sm.text().replace("历史摘要：", "");
            }
        }

        StringBuilder content = new StringBuilder();
        for (ChatMessage msg : toCompress) {
            if (msg instanceof UserMessage um) {
                content.append("用户: ").append(um.singleText()).append("\n");
            } else if (msg instanceof AiMessage am && am.text() != null) {
                content.append("助手: ").append(am.text()).append("\n");
            }
        }

        try {
            String newSummaryPart = getOrCreateModel().chat("""
                    以下是需要总结的对话内容，请用简洁的要点提炼关键信息(用户身份、偏好、约定等)。
                    %s
                    """.formatted(content));

            String merged;
            if (existingSummary.isEmpty()) {
                merged = newSummaryPart;
            } else {
                merged = getOrCreateModel().chat("""
                        请合并以下两段关于用户的摘要，去重并保留所有重要信息:
                        --- 旧摘要 ---
                        %s
                        --- 新片段 ---
                        %s
                        """.formatted(existingSummary, newSummaryPart));
            }

            sa.memory.clear();
            sa.memory.add(new SystemMessage("历史摘要：" + merged));
            for (ChatMessage msg : keep) {
                sa.memory.add(msg);
            }
            sessionStore.saveSummary(sessionId, sa.session.getName(), merged);
            log.info("会话 {} 滑动压缩完成, 消息 {} -> 摘要+{}", sessionId, rawMessages.size(), keep.size());
        } catch (Exception e) {
            log.error("压缩失败: {}", e.getMessage());
        }
    }
}
