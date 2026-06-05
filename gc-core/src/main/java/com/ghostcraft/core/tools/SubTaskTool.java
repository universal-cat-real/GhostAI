package com.ghostcraft.core.tools;

import com.ghostcraft.core.conversation.ConversationManager;
import com.ghostcraft.core.subtask.SubTask;
import com.ghostcraft.core.subtask.SubTaskManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubTaskTool implements GhostTool {

    @Autowired
    private SubTaskManager subTaskManager;

    @Autowired
    private ConversationManager conversationManager;

    @Tool("创建一个子任务让另一个 AI 独立处理，传入任务描述和详细指令，子任务会异步执行，完成后自动通知")
    public String createSubTask(
            @P("任务描述，简短说明") String description,
            @P("给子任务 AI 的详细指令") String prompt) {
        SubTask task = subTaskManager.createTask(description, prompt);
        String sessionId = conversationManager.getActiveSessionId();
        subTaskManager.executeAsync(task, sessionId);
        return "子任务: " + description + " 已在后台执行，完成后会自动通知你。你可以继续聊其他话题。";
    }

    @Tool("列出所有子任务及其状态")
    public String listSubTasks() {
        var list = subTaskManager.listAll();
        if (list.isEmpty()) return "暂无子任务";
        StringBuilder sb = new StringBuilder("子任务列表：\n");
        for (SubTask t : list) {
            sb.append("  [").append(t.getId()).append("] ")
                    .append(t.getDescription()).append(" -- ").append(t.getStatus()).append("\n");
        }
        return sb.toString();
    }
}
