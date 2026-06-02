package com.ghostcraft.mcp;

import com.ghostcraft.core.skill.Skill;
import com.ghostcraft.core.skill.SkillRegistry;
import com.ghostcraft.mcp.tools.McpTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class McpSkill implements Skill {

    private static final Logger log = LoggerFactory.getLogger(McpSkill.class);

    @Autowired
    private McpClient mcpClient;

    private List<McpTool> remoteTools = List.of();

    @Override
    public String name() { return "mcp"; }

    @Override
    public String description() {
        return "MCP 远程工具 (" + remoteTools.size() + " 个)";
    }

    @Override
    public Object toolInstance() { return this; }

    public void connect(String serverUrl) {
        mcpClient.connect(serverUrl);
        refreshTools();
    }

    public void refreshTools() {
        if (!mcpClient.isConnected()) return;
        remoteTools = mcpClient.listTools();
        log.info("MCP 已加载 {} 个远程工具", remoteTools.size());
    }

    @Tool("调用远程 MCP 工具，传入工具名和 JSON 参数")
    public String callMcpTool(
            @P("工具名称") String toolName,
            @P("JSON 格式的参数") String arguments) {
        return mcpClient.callTool(toolName, arguments);
    }
}
