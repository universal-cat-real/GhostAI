package com.ghostcraft.core.hook;

import com.ghostcraft.core.security.PermissionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HookRegistry {

    private static final Logger log = LoggerFactory.getLogger(HookRegistry.class);
    private final List<Hook> hooks = new ArrayList<>();

    @Autowired
    private PermissionManager permissionManager;

    public void register(Hook hook) {
        hooks.add(hook);
        log.info("已注册钩子: {}", hook.name());
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

    /**
     * 工具权限检查
     * @param sessionId
     * @param toolName
     * @param arguments
     * @return
     */
    public boolean fireOnToolCall(String sessionId, String toolName, String arguments) {
        String reason = permissionManager.checkPermission(toolName);
        if (reason != null) {
            log.warn("工具调用被拦截: {} — {}", toolName, reason);
            System.out.println("  [安全] " + reason);
            return false;
        }
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

    public List<Hook> list() { return List.copyOf(hooks); }
    public int count() { return hooks.size(); }
}
