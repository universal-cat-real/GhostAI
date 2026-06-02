package com.ghostcraft.hook;

import java.util.ArrayList;
import java.util.List;

/**
 * 钩子注册器
 *
 * 作用：管理所有钩子，按阶段依次触发。
 */
public class HookRegistry {

    private final List<Hook> hooks = new ArrayList<>();

    /** 注册一个钩子 */
    public void register(Hook hook) {
        hooks.add(hook);
        System.out.println("  [钩子] 已注册: " + hook.name());
    }

    /** 触发 beforeChat */
    public void fireBeforeChat(String sessionId, String userMessage) {
        for (Hook hook : hooks) {
            hook.beforeChat(sessionId, userMessage);
        }
    }

    /** 触发 afterChat */
    public void fireAfterChat(String sessionId, String userMessage, String aiMessage) {
        for (Hook hook : hooks) {
            hook.afterChat(sessionId, userMessage, aiMessage);
        }
    }

    /** 触发 onToolCall，任一钩子返回 false 则阻止执行 */
    public boolean fireOnToolCall(String sessionId, String toolName, String arguments) {
        for (Hook hook : hooks) {
            if (!hook.onToolCall(sessionId, toolName, arguments)) {
                return false;
            }
        }
        return true;
    }

    /** 触发 onError */
    public void fireOnError(String sessionId, String userMessage, Throwable error) {
        for (Hook hook : hooks) {
            hook.onError(sessionId, userMessage, error);
        }
    }

    /** 获取所有已注册的钩子 */
    public List<Hook> list() {
        return List.copyOf(hooks);
    }

    /** 钩子数量 */
    public int count() {
        return hooks.size();
    }
}
