package com.ghostcraft.memory.config;

import com.ghostcraft.core.conversation.ConversationManager;
import com.ghostcraft.core.hook.HookRegistry;
import com.ghostcraft.memory.store.MemoryPersistHook;
import com.ghostcraft.memory.store.MemoryStore;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemoryAutoRegister {

    private static final Logger log = LoggerFactory.getLogger(MemoryAutoRegister.class);

    @Autowired
    private HookRegistry hookRegistry;

    @Autowired
    private MemoryStore memoryStore;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ConversationManager conversationManager;

    @PostConstruct
    public void init() {
        hookRegistry.register(new MemoryPersistHook(memoryStore, chatModel, conversationManager));
        log.info("MemoryPersistHook 已自动注册");
    }
}
