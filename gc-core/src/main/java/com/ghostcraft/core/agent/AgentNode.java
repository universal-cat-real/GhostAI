package com.ghostcraft.core.agent;

import com.ghostcraft.core.conversation.ConversationManager;
import dev.langchain4j.memory.ChatMemory;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 节点 — 树形结构中的一环
 *
 * Map 存节点（扁平），parentId 表达层级，children 运行时拼出树。
 */
public class AgentNode {

    public final String id;
    public final String name;
    public final String parentId;               // null 表示根节点
    public final String systemPrompt;
    public final transient ConversationManager.Assistant agent;  // 运行时，AiServices 实例
    public final transient ChatMemory memory;                    // 运行时，独立记忆
    public transient List<AgentNode> children;                   // 运行时，拼出来的子树

    public AgentNode(String id, String name, String parentId, String systemPrompt,
                     ConversationManager.Assistant agent, ChatMemory memory) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.systemPrompt = systemPrompt;
        this.agent = agent;
        this.memory = memory;
        this.children = new ArrayList<>();
    }
}
