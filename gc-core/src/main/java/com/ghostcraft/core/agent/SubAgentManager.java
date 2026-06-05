package com.ghostcraft.core.agent;

import com.ghostcraft.core.conversation.ConversationManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SubAgentManager {

    private static final Logger log = LoggerFactory.getLogger(SubAgentManager.class);
    private static final Path AGENTS_DIR = Paths.get(System.getProperty("user.home"), ".ghostcraft", "agents");

    @Autowired
    private ChatModel chatModel;

    private final Map<String, AgentNode> agents = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            Files.createDirectories(AGENTS_DIR);
            loadFromDisk();
        } catch (Exception e) {
            log.warn("创建 agents 目录失败: {}", e.getMessage());
        }
    }

    /** 创建子 Agent（自动生成 ID） */
    public AgentNode createAgent(String name, String parentId, String systemPrompt) {
        return createAgentWithId(UUID.randomUUID().toString().substring(0, 8), name, parentId, systemPrompt);
    }

    /** 创建子 Agent（指定 ID，用于创建根 Agent 时与会话 ID 保持一致） */
    public AgentNode createAgentWithId(String id, String name, String parentId, String systemPrompt) {
        ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
        ConversationManager.Assistant agent = AiServices.builder(ConversationManager.Assistant.class)
                .chatModel(chatModel)
                .chatMemory(memory)
                .systemMessage(systemPrompt)
                .build();

        AgentNode node = new AgentNode(id, name, parentId, systemPrompt, agent, memory);
        agents.put(id, node);
        saveToDisk(node);
        log.info("子 Agent 已创建: {} [{}] 挂载在 {}", name, id, parentId);
        return node;
    }

    public String chatWithAgent(String agentId, String message) {
        AgentNode node = agents.get(agentId);
        if (node == null) return "子 Agent 不存在: " + agentId;
        return node.agent.chat(message);
    }

    public TreeNode buildTree(String rootId) {
        AgentNode root = agents.get(rootId);
        if (root == null) return null;
        return buildTreeNode(root);
    }

    private TreeNode buildTreeNode(AgentNode node) {
        TreeNode tn = new TreeNode(node.id, node.name, node.parentId);
        for (AgentNode child : findChildren(node.id)) {
            tn.children.add(buildTreeNode(child));
        }
        return tn;
    }

    public List<AgentNode> findChildren(String parentId) {
        return agents.values().stream()
                .filter(n -> parentId != null && parentId.equals(n.parentId))
                .toList();
    }

    public List<AgentNode> lineage(String agentId) {
        List<AgentNode> result = new ArrayList<>();
        AgentNode current = agents.get(agentId);
        while (current != null) {
            result.add(current);
            current = agents.get(current.parentId);
        }
        return result;
    }

    public List<AgentNode> listAll() { return List.copyOf(agents.values()); }

    /** 向上追溯根节点 */
    public AgentNode findRoot(String agentId) {
        AgentNode current = agents.get(agentId);
        while (current != null && current.parentId != null) {
            current = agents.get(current.parentId);
        }
        return current;
    }

    /** 获取指定会话的根 Agent */
    public AgentNode getSessionRoot(String sessionId) {
        return agents.get(sessionId);
    }

    /** 列出指定会话下的所有子 Agent（不含根） */
    public List<AgentNode> listSessionChildren(String sessionId) {
        return agents.values().stream()
                .filter(n -> sessionId.equals(n.parentId))
                .toList();
    }

    /** 检查指定 Agent 是否属于当前会话 */
    public boolean belongsToSession(String agentId, String sessionId) {
        AgentNode root = findRoot(agentId);
        return root != null && root.id.equals(sessionId);
    }

    public List<AgentNode> listRoots() {
        return agents.values().stream().filter(n -> n.parentId == null).toList();
    }

    public AgentNode get(String agentId) { return agents.get(agentId); }

    private void saveToDisk(AgentNode node) {
        try {
            var map = new LinkedHashMap<String, Object>();
            map.put("id", node.id);
            map.put("name", node.name);
            map.put("parentId", node.parentId);
            map.put("systemPrompt", node.systemPrompt);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(
                    AGENTS_DIR.resolve(node.id + ".json").toFile(), map);
        } catch (Exception e) {
            log.warn("持久化子 Agent [{}] 失败: {}", node.id, e.getMessage());
        }
    }

    private void loadFromDisk() {
        File[] files = AGENTS_DIR.toFile().listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;
        ObjectMapper mapper = new ObjectMapper();
        for (File file : files) {
            try {
                Map<String, Object> data = mapper.readValue(file, new TypeReference<>() {});
                String id = (String) data.get("id");
                String name = (String) data.get("name");
                String parentId = (String) data.get("parentId");
                String systemPrompt = (String) data.get("systemPrompt");
                if (id != null && systemPrompt != null) {
                    ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
                    ConversationManager.Assistant agent = AiServices.builder(ConversationManager.Assistant.class)
                            .chatModel(chatModel)
                            .chatMemory(memory)
                            .systemMessage(systemPrompt)
                            .build();
                    agents.put(id, new AgentNode(id, name, parentId, systemPrompt, agent, memory));
                }
            } catch (Exception e) {
                log.warn("加载 Agent 文件 [{}] 失败: {}", file.getName(), e.getMessage());
            }
        }
        log.info("已从磁盘加载 {} 个子 Agent", agents.size());
    }

    public static class TreeNode {
        public final String id;
        public final String name;
        public final String parentId;
        public final List<TreeNode> children = new ArrayList<>();
        public TreeNode(String id, String name, String parentId) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
        }
    }
}
