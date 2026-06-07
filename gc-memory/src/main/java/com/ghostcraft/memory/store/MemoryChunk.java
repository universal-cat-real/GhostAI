package com.ghostcraft.memory.store;

import java.time.LocalDateTime;
import java.util.UUID;

public class MemoryChunk {

    private final String id;
    private final String sessionId;
    private final String content;
    private String keywords;
    private float[] embedding;
    private final LocalDateTime createdAt;
    private double score; // 运行时排序用

    public MemoryChunk(String sessionId, String content) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.sessionId = sessionId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public String id() { return id; }
    public String sessionId() { return sessionId; }
    public String content() { return content; }
    public String keywords() { return keywords; }
    public float[] embedding() { return embedding; }
    public LocalDateTime createdAt() { return createdAt; }
    public double score() { return score; }

    public void setKeywords(String keywords) { this.keywords = keywords; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public void setScore(double score) { this.score = score; }
}
