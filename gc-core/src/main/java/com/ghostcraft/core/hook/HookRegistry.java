package com.ghostcraft.core.hook;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 钩子注册器
 *
 * 作用：管理所有钩子，按阶段依次触发。
 */
@Component
public class HookRegistry {

    private final List<Hook> hooks = new ArrayList<>();

    public void register(Hook hook) {
        hooks.add(hook);
        System.out.println("  [钩子] 已注册: " + hook.name());
    }

    public void fireBeforeChat(String sessionId, String userMessage) {
        for (Hook hook : hooks) {
            hook.beforeChat(sessionId, userMessage);
        }
    }

    public void fireAfterChat(String sessionId, String userMessage, String aiMessage) {
        for (Hook hook : hooks) {
            hook.afterChat(sessionId, userMessage, aiMessage);
        }
    }

    public boolean fireOnToolCall(String sessionId, String toolName, String arguments) {
        for (Hook hook : hooks) {
            if (!hook.onToolCall(sessionId, toolName, arguments)) {
                return false;
            }
        }
        return true;
    }

    public void fireOnError(String sessionId, String userMessage, Throwable error) {
        for (Hook hook : hooks) {
            hook.onError(sessionId, userMessage, error);
        }
    }

    public List<Hook> list() {
        return List.copyOf(hooks);
    }

    public int count() {
        return hooks.size();
    }
}
