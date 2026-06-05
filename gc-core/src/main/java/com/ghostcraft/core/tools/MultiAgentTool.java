package com.ghostcraft.core.tools;

import com.ghostcraft.core.agent.AgentNode;
import com.ghostcraft.core.agent.SubAgentManager;
import com.ghostcraft.core.conversation.ConversationManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MultiAgentTool implements GhostTool {

    @Autowired
    private SubAgentManager subAgentManager;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ConversationManager conversationManager;

    @Tool("创建一个子Agent并挂载到当前主Agent下，传入对子Agent的行为、角色描述，返回创建结果")
    public String createSubAgent(@P("对子Agent的行为、角色、人设的描述") String description) {
        String systemPrompt = chatModel.chat("""
                根据以下描述，生成一段 AI 助手的系统提示词，用来创建子 Agent。
                要求：用中文，第一人称，说明身份、能力边界、说话风格。
                只输出提示词本身，不要额外内容。
                
                描述：%s
                """.formatted(description));

        String name = description.length() > 15 ? description.substring(0, 15) + "..." : description;
        String parentId = conversationManager.getRootAgentId();
        var node = subAgentManager.createAgent(name, parentId, systemPrompt);
        return "子 Agent 已创建:\nID: " + node.id + "\n名称: " + name + "\n人设:\n" + systemPrompt;
    }

    @Tool("跟指定 ID 的子 Agent 对话，传入子 Agent ID 和消息内容")
    public String chatWithSubAgent(@P("子 Agent 的 ID") String agentId,
                                    @P("发给子 Agent 的消息") String message) {
        String sessionId = conversationManager.getRootAgentId();
        if (!subAgentManager.belongsToSession(agentId, sessionId)) {
            return "该子 Agent 不属于当前会话";
        }
        return subAgentManager.chatWithAgent(agentId, message);
    }

    @Tool("列出所有子 Agent 及其层级关系")
    public String listSubAgents() {
        String sessionId = conversationManager.getRootAgentId();
        var tree = subAgentManager.buildTree(sessionId);
        if (tree == null) return "没有子 Agent";
        StringBuilder sb = new StringBuilder("子 Agent 树状结构：\n");
        printTree(sb, tree, 0);
        return sb.toString();
    }

    @Tool("根据任务描述，查找能力匹配的子agent，返回最佳匹配的子agent")
    public String findAndMatchAgent(@P("任务的描述") String desc){
        String activeSessionId = conversationManager.getActiveSessionId();
        List<AgentNode> agentNodes = subAgentManager.listSessionChildren(activeSessionId);
        if(agentNodes == null || agentNodes.size() == 0) return "该主agent没有子agent可使用";

        // 把每个子 Agent 的人设和任务描述一起发给 LLM 匹配
        String prompt = "从以下子 Agent 中选出最合适执行这个任务的：\n任务：" + desc + "\n\n";
        for (var agent : agentNodes) {
            prompt += "ID: " + agent.id + " 角色: " + agent.systemPrompt.substring(0, 100) + "\n";
        }
        prompt += "\n只返回最合适的 Agent ID，没有匹配返回 null";
        return chatModel.chat(prompt);
    }

    @Tool("派发任务给指定的ID的子agent来执行，返回执行结果")
    public String assignTaskToAgent(@P("任务内容") String task,
                                    @P("子 Agent ID") String agentId){
        return subAgentManager.chatWithAgent(agentId, task);
    }

    private void printTree(StringBuilder sb, SubAgentManager.TreeNode node, int depth) {
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append("[").append(node.id).append("] ").append(node.name).append("\n");
        for (var child : node.children) {
            printTree(sb, child, depth + 1);
        }
    }
}
