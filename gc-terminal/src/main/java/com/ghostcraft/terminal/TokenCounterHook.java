package com.ghostcraft.terminal;

import com.ghostcraft.core.hook.Hook;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenCounterHook implements Hook {

    private final Map<String, long[]> stats = new ConcurrentHashMap<>();

    @Override
    public void beforeChat(String sessionId, String userMessage) {
        long[] s = stats.computeIfAbsent(sessionId, k -> new long[4]);
        s[0]++;                    // 消息数 +1
        s[2] += userMessage.length();  // 用户字符数
    }

    @Override
    public void afterChat(String sessionId, String userMessage, String aiMessage) {
        long[] s = stats.get(sessionId);
        if (s != null && aiMessage != null) {
            s[3] += aiMessage.length();  // AI 字符数
            // 粗略估算 token 数：中文字符 * 2 + 英文单词 * 1.3
            s[1] = (long)(s[2] * 2 + s[3] * 1.3);
        }
    }

    /** 获取某个会话的统计 */
    public String getStats(String sessionId) {
        long[] s = stats.get(sessionId);
        if (s == null) return "无数据";
        return "消息数: " + s[0]
                + " | 估算 token: " + s[1]
                + " | 用户输入: " + s[2] + " 字"
                + " | AI 回复: " + s[3] + " 字";
    }

    @Override
    public String name() {
        return "TokenCounter";
    }
}
