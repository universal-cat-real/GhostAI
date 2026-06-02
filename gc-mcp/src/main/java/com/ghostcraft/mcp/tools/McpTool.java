package com.ghostcraft.mcp.tools;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * MCP 工具描述 — 从远程服务返回的工具定义
 */
@Data
@AllArgsConstructor
@ToString
public class McpTool {
    /**
     * MCP名称
     */
    private final String name;

    /**
     * MCP描述
     */
    private final String description;

    /**
     * 参数json字符串
     */
    private final String parametersJson;

}
