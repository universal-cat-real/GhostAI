package com.ghostcraft.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PermissionManager {

    private static final Logger log = LoggerFactory.getLogger(PermissionManager.class);
    private final Map<String, ToolPolicy> policies = new ConcurrentHashMap<>();
    private ToolPolicy defaultPolicy = ToolPolicy.ALLOW;

    public PermissionManager() {
        policies.put("createSubTask", ToolPolicy.ALLOW);
        policies.put("listSubTasks", ToolPolicy.ALLOW);
        policies.put("listSessions", ToolPolicy.ALLOW);
        policies.put("switchSession", ToolPolicy.ALLOW);
        policies.put("createSession", ToolPolicy.ALLOW);
        policies.put("callMcpTool", ToolPolicy.ASK);
    }

    public void setPolicy(String toolName, ToolPolicy policy) {
        policies.put(toolName, policy);
        log.info("权限策略已更新: {} = {}", toolName, policy);
    }

    public void setDefaultPolicy(ToolPolicy policy) {
        this.defaultPolicy = policy;
    }

    public String checkPermission(String toolName) {
        ToolPolicy policy = policies.getOrDefault(toolName, defaultPolicy);
        return switch (policy) {
            case ALLOW -> null;
            case DENY -> "工具 [" + toolName + "] 已被管理员禁止使用";
            case ASK -> askUser(toolName);
        };
    }

    private String askUser(String toolName) {
        System.out.print("\n  [权限] Agent 想调用工具 [" + toolName + "]，是否允许？(y/n): ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine().trim().toLowerCase();
        if ("y".equals(input) || "yes".equals(input)) {
            return null;
        }
        return "用户拒绝了工具调用: " + toolName;
    }

    public String listPolicies() {
        StringBuilder sb = new StringBuilder("工具权限策略：\n");
        sb.append("  默认策略: ").append(defaultPolicy).append("\n");
        for (var entry : policies.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
