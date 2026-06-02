package com.ghostcraft.core.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenCounterHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(TokenCounterHook.class);
    private final Map<String, long[]> stats = new ConcurrentHashMap<>();

    @Override
    public void beforeChat(String sessionId, String userMessage) {
        long[] s = stats.computeIfAbsent(sessionId, k -> new long[4]);
        s[0]++;                          // 消息数
        s[2] += userMessage.length();    // 用户字符数
    }

    @Override
    public void afterChat(String sessionId, String userMessage, String aiMessage) {
        long[] s = stats.get(sessionId);
        if (s != null && aiMessage != null) {
            s[3] += aiMessage.length();  // AI 字符数
            s[1] = (long)(s[2] * 2 + s[3] * 1.3);  // 粗估 token
            if (s[1] > 0 && s[1] % 500 == 0) {
                log.warn("会话 {} token 已达 {}, 建议压缩", sessionId, s[1]);
            }
        }
    }

    public String getStats(String sessionId) {
        long[] s = stats.get(sessionId);
        if (s == null) return "无数据";
        return "消息: " + s[0] + " | 估算token: " + s[1]
                + " | 用户输入: " + s[2] + " 字 | AI回复: " + s[3] + " 字";
    }

    @Override
    public String name() { return "TokenCounter"; }
}
