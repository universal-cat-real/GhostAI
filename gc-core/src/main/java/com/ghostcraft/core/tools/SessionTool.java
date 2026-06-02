package com.ghostcraft.core.tools;

import com.ghostcraft.core.conversation.ConversationManager;
import com.ghostcraft.core.model.Session;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SessionTool {

    @Autowired
    private ConversationManager conversationManager;

    @Tool("获取当前所有活跃会话列表，返回每个会话的 ID 和名称")
    public String listSessions() {
        List<Session> sessions = conversationManager.listSessions();
        if (sessions.isEmpty()) return "当前没有活跃会话";
        StringBuilder sb = new StringBuilder("当前会话列表：\n");
        String activeId = conversationManager.getActiveSessionId();
        for (Session s : sessions) {
            String marker = s.getId().equals(activeId) ? " [当前]" : "";
            sb.append("  ID: ").append(s.getId())
                    .append(" | 名称: ").append(s.getName())
                    .append(marker).append("\n");
        }
        return sb.toString();
    }

    @Tool("切换到指定 ID 的会话，后续对话将在该会话中继续")
    public String switchSession(@P("要切换到的会话 ID") String sessionId) {
        boolean exists = conversationManager.listSessions().stream()
                .anyMatch(s -> s.getId().equals(sessionId));
        if (!exists) {
            return "会话不存在: " + sessionId;
        }
        conversationManager.setActiveSessionId(sessionId);
        return "已切换到会话: " + sessionId;
    }

    @Tool("创建一个新的会话，并自动切换过去")
    public String createSession(@P("新会话的名称") String name) {
        String id = conversationManager.createSession(name);
        return "已创建并切换到新会话 [" + name + "]，ID: " + id;
    }
}
