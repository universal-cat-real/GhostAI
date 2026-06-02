package com.ghostcraft.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostcraft.mcp.tools.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private String serverUrl;

    public void connect(String serverUrl) {
        this.serverUrl = serverUrl;
        log.info("MCP 客户端连接到: {}", serverUrl);
    }

    public boolean isConnected() { return serverUrl != null; }

    /**
     * 获取Mcp下工具列表
     * @return
     */
    public List<McpTool> listTools() {
        if (serverUrl == null) return List.of();
        try {
            // 发送http请求，获取工具信息
            String json = post("/tools/list", "{}");

            // 解析工具信息
            Map<String, Object> resp = mapper.readValue(json, new TypeReference<>() {});

            // 拿到工具信息里的tool
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tools = (List<Map<String, Object>>) resp.get("tools");
            if (tools == null) return List.of();
            return tools.stream().map(t -> {
                try {
                    return new McpTool(
                            (String) t.get("name"),
                            (String) t.getOrDefault("description", ""),
                            mapper.writeValueAsString(t.getOrDefault("parameters", Map.of()))
                    );
                } catch (Exception e) {
                    return new McpTool(
                            (String) t.get("name"),
                            (String) t.getOrDefault("description", ""),
                            "{}");
                }
            }).toList();
        } catch (Exception e) {
            log.warn("获取 MCP 工具列表失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 调用Mcp工具
     * @param toolName
     * @param argumentsJson
     * @return
     */
    public String callTool(String toolName, String argumentsJson) {
        if (serverUrl == null) return "MCP 未连接";
        try {
            // 构建http请求体
            String body = mapper.writeValueAsString(Map.of(
                    "name", toolName,
                    "arguments", mapper.readValue(argumentsJson, Map.class)
            ));
            // 发起调用并返回执行结果
            return post("/tools/call", body);
        } catch (Exception e) {
            return "MCP 工具调用失败: " + e.getMessage();
        }
    }

    /**
     * 发起HTTP请求
     * @param path
     * @param body
     * @return
     * @throws Exception
     */
    private String post(String path, String body) throws Exception {
        URL url = new URL(serverUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        byte[] resp = conn.getInputStream().readAllBytes();
        return new String(resp, StandardCharsets.UTF_8);
    }
}
