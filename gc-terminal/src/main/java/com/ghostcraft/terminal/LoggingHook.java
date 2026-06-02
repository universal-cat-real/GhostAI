package com.ghostcraft.terminal;

import com.ghostcraft.core.hook.Hook;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * 日志钩子 — 记录每次对话的时间、内容长度
 *
 * 作用：演示 Hook 怎么写。每段对话都会打印日志。
 */
@Component
public class LoggingHook implements Hook {

    @Override
    public void beforeChat(String sessionId, String userMessage) {
        System.out.println("  [日志] 会话 " + sessionId
                + " | 时间 " + LocalTime.now()
                + " | 用户消息 " + userMessage.length() + " 字");
    }

    @Override
    public void afterChat(String sessionId, String userMessage, String aiMessage) {
        System.out.println("  [日志] 会话 " + sessionId
                + " | 回答 " + (aiMessage != null ? aiMessage.length() : 0) + " 字");
    }

    @Override
    public void onError(String sessionId, String userMessage, Throwable error) {
        System.out.println("  [日志] 会话 " + sessionId + " | 错误: " + error.getMessage());
    }
}
