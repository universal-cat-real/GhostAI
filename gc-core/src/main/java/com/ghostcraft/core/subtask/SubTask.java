package com.ghostcraft.core.subtask;

import java.time.LocalDateTime;
import java.util.UUID;

public class SubTask {

    private final String id;
    private final String description;
    private final String prompt;
    private Status status;
    private String result;
    private final LocalDateTime createdAt;

    public enum Status { PENDING, RUNNING, DONE, FAILED }

    public SubTask(String description, String prompt) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.description = description;
        this.prompt = prompt;
        this.status = Status.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getPrompt() { return prompt; }
    public Status getStatus() { return status; }
    public String getResult() { return result; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void start() { this.status = Status.RUNNING; }
    public void complete(String result) { this.status = Status.DONE; this.result = result; }
    public void fail(String error) { this.status = Status.FAILED; this.result = error; }
}
