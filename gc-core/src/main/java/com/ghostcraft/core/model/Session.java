package com.ghostcraft.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 会话数据模型
 *
 * 每个会话都有一个独立的id
 */
@Data
@AllArgsConstructor
@ToString
public class Session {

    /**
     * 会话id
     */
    private final String id;

    /**
     * 会话名称
     */
    private final String name;

    /**
     * 创建时间
     */
    private final LocalDateTime created;

    /**
     * 最后活跃时间
     */
    private LocalDateTime updated;

    public Session(String name) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.created = LocalDateTime.now();
        this.updated = LocalDateTime.now();
    }

    /**
     * 更新最后活跃时间
     */
    public void touch() {
        this.updated = LocalDateTime.now();
    }
}
