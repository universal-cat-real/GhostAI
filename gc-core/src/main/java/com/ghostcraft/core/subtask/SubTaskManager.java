package com.ghostcraft.core.subtask;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

@Component
public class SubTaskManager {

    private static final Logger log = LoggerFactory.getLogger(SubTaskManager.class);
    private final Map<String, SubTask> subTasks = new ConcurrentHashMap<>();

    /** 已完成任务的通知队列: sessionId -> [任务ID, 结果] */
    private final Queue<CompletionEvent> completionEvents = new ConcurrentLinkedQueue<>();

    @Value("${ghostcraft.api-key}")
    private String apiKey;

    public record CompletionEvent(String sessionId, String taskId, String description, String result) {}

    public SubTask createTask(String description, String prompt) {
        SubTask task = new SubTask(description, prompt);
        subTasks.put(task.getId(), task);
        return task;
    }

    public CompletableFuture<SubTask> executeAsync(SubTask task, String sessionId) {
        task.start();
        return CompletableFuture.supplyAsync(() -> {
            try {
                ChatModel model = OpenAiChatModel.builder()
                        .apiKey(apiKey).baseUrl("https://api.deepseek.com")
                        .modelName("deepseek-chat").temperature(0.3)
                        .build();
                String answer = model.chat(task.getPrompt());
                task.complete(answer);
                // 推入完成事件队列
                completionEvents.add(new CompletionEvent(
                        sessionId, task.getId(), task.getDescription(), answer));
                log.info("子任务 {} 完成: {}", task.getId(), task.getDescription());
            } catch (Exception e) {
                task.fail(e.getMessage());
                completionEvents.add(new CompletionEvent(
                        sessionId, task.getId(), task.getDescription(), "失败: " + e.getMessage()));
                log.warn("子任务 {} 失败: {}", task.getId(), e.getMessage());
            }
            return task;
        });
    }

    /** 消费指定会话的完成事件 */
    public CompletionEvent pollCompletion(String sessionId) {
        for (var it = completionEvents.iterator(); it.hasNext();) {
            var event = it.next();
            if (event.sessionId().equals(sessionId)) {
                it.remove();
                return event;
            }
        }
        return null;
    }

    public SubTask getTask(String id) { return subTasks.get(id); }
    public List<SubTask> listAll() { return List.copyOf(subTasks.values()); }
    public int count() { return subTasks.size(); }
}
