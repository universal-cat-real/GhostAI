package com.ghostcraft.core.hook;

import com.ghostcraft.core.conversation.ConversationManager;
import org.springframework.stereotype.Component;

/**
 * 每 5 轮对话自动持久化会话摘要
 */
@Component
public class PersistHook implements Hook {

    private final ConversationManager conversationManager;

    public PersistHook(ConversationManager conversationManager) {
        this.conversationManager = conversationManager;
    }

    @Override
    public void afterChat(String sessionId, String userMessage, String aiMessage) {
        conversationManager.persistIfDue(sessionId);
    }

    @Override
    public String name() { return "PersistHook"; }
}
