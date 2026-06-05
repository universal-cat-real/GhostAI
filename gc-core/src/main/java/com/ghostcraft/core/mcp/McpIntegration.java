package com.ghostcraft.core.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class McpIntegration {

    private static final Logger log = LoggerFactory.getLogger(McpIntegration.class);
    private static final Path SKILL_DIR = Paths.get(System.getProperty("user.home"), ".ghostcraft", "skills");

    private final List<McpClient> clients = new ArrayList<>();

    public McpToolProvider scanAndConnect() {
        File[] dirs = SKILL_DIR.toFile().listFiles(File::isDirectory);
        if (dirs == null) return null;

        List<McpClient> discovered = new ArrayList<>();

        for (File dir : dirs) {
            File mcpFile = new File(dir, "mcp_server.py");
            if (!mcpFile.exists()) continue;

            String skillName = dir.getName();
            log.info("发现 MCP 技能 [{}]，启动 mcp_server.py", skillName);

            try {
                StdioMcpTransport transport = new StdioMcpTransport.Builder()
                        .command(List.of("python", mcpFile.getAbsolutePath()))
                        .build();

                McpClient client = new DefaultMcpClient.Builder()
                        .transport(transport)
                        .build();
                discovered.add(client);
                clients.add(client);
                log.info("MCP 技能 [{}] 连接成功", skillName);
            } catch (Exception e) {
                log.warn("连接 MCP 技能 [{}] 失败: {}", skillName, e.getMessage());
            }
        }

        if (discovered.isEmpty()) return null;

        return McpToolProvider.builder()
                .mcpClients(discovered.stream().map(c -> (McpClient)c).toList())
                .build();
    }

    @PreDestroy
    public void shutdown() {
        for (var client : clients) {
            try { client.close(); } catch (Exception e) {
                log.warn("关闭 MCP 客户端失败: {}", e.getMessage());
            }
        }
    }
}
