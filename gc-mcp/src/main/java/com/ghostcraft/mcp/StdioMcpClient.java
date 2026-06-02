package com.ghostcraft.mcp;

import com.ghostcraft.mcp.tools.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stdio MCP 客户端
 *
 * 作用：启动一个子进程 MCP 服务器（如 GitHub MCP），通过 stdin/stdout 通信。
 * 通信协议：JSON-RPC 2.0
 */
@Component
public class StdioMcpClient {

    private static final Logger log = LoggerFactory.getLogger(StdioMcpClient.class);

    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final AtomicInteger requestId = new AtomicInteger(1);
    private final Map<Integer, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    private Thread readThread;
    private volatile boolean running;
    private String serverCommand;

    /**
     * 启动 MCP 子进程
     *
     * @param command 启动命令，如 "npx @modelcontextprotocol/server-github"
     */
    public void connect(String command) {
        this.serverCommand = command;
        try {
            // 按空格拆分成命令和参数
            String[] parts = command.split(" ");
            ProcessBuilder pb = new ProcessBuilder(parts);
            pb.redirectErrorStream(false);
            // 单独捕获 stderr
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            process = pb.start();

            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

            running = true;

            // 启动读取线程：持续从 stdout 读取响应
            readThread = new Thread(this::readLoop, "mcp-reader");
            readThread.setDaemon(true);
            readThread.start();

            log.info("MCP stdio 客户端已启动: {}", command);
        } catch (IOException e) {
            throw new RuntimeException("启动 MCP 子进程失败: " + command, e);
        }
    }

    public boolean isConnected() { return running && process != null && process.isAlive(); }

    public void disconnect() {
        running = false;
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                process.waitFor(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            process.destroyForcibly();
        }
        log.info("MCP stdio 客户端已断开");
    }

    // ═══════════════════════════════ MCP 协议方法 ═══════════════════════════════

    /**
     * 发送 initialize 请求（MCP 协议的第一步）
     */
    public void initialize() {
        int id = requestId.getAndIncrement();
        String req = jsonRpcRequest(id, "initialize", Map.of(
                "protocolVersion", "2025-03-26",
                "clientInfo", Map.of("name", "ghostcraft", "version", "1.0.0")
        ));
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        send(req);
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("MCP initialize 超时或失败: {}", e.getMessage());
        }
    }

    /**
     * 获取工具列表
     */
    public List<McpTool> listTools() {
        int id = requestId.getAndIncrement();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        send(jsonRpcRequest(id, "tools/list", Map.of()));

        try {
            String resultJson = future.get(15, TimeUnit.SECONDS);
            // 解析结果
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> result = mapper.readValue(resultJson, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
            if (tools == null) return List.of();
            return tools.stream().map(t -> {
                try {
                    return new McpTool(
                            (String) t.get("name"),
                            (String) t.getOrDefault("description", ""),
                            mapper.writeValueAsString(t.getOrDefault("inputSchema", Map.of()))
                    );
                } catch (Exception e) {
                    return new McpTool((String) t.get("name"), "", "{}");
                }
            }).toList();
        } catch (Exception e) {
            log.warn("获取 MCP 工具列表失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 调用一个工具
     */
    public String callTool(String toolName, String argumentsJson) {
        int id = requestId.getAndIncrement();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> args = mapper.readValue(argumentsJson, Map.class);
            send(jsonRpcRequest(id, "tools/call", Map.of("name", toolName, "arguments", args)));

            String resultJson = future.get(30, TimeUnit.SECONDS);
            Map<String, Object> result = mapper.readValue(resultJson, Map.class);

            // MCP 返回格式：{ content: [{ type: "text", text: "..." }] }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            if (content != null) {
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> c : content) {
                    Object text = c.get("text");
                    if (text != null) sb.append(text);
                }
                return sb.toString();
            }
            return resultJson;
        } catch (Exception e) {
            return "MCP 工具调用失败: " + e.getMessage();
        }
    }

    // ═══════════════════════════════ 内部 ═══════════════════════════════

    private void send(String json) {
        try {
            writer.write(json);
            writer.newLine();
            writer.flush();
            log.debug("MCP 发送: {}", json);
        } catch (IOException e) {
            log.warn("MCP 发送失败: {}", e.getMessage());
        }
    }

    private String jsonRpcRequest(int id, String method, Object params) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "method", method,
                    "params", params
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                processLine(line);
            }
        } catch (IOException e) {
            if (running) {
                log.warn("MCP 读取线程异常: {}", e.getMessage());
            }
        }
    }

    private void processLine(String line) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> msg = mapper.readValue(line, Map.class);

            // JSON-RPC 响应包含 id 字段
            if (msg.containsKey("id")) {
                int id = ((Number) msg.get("id")).intValue();
                CompletableFuture<String> future = pendingRequests.remove(id);
                if (future != null) {
                    if (msg.containsKey("result")) {
                        future.complete(mapper.writeValueAsString(msg.get("result")));
                    } else if (msg.containsKey("error")) {
                        future.completeExceptionally(
                                new RuntimeException("MCP 错误: " + msg.get("error")));
                    }
                }
            } else if ("notifications/initialized".equals(msg.get("method"))) {
                log.info("MCP 初始化完成通知已收到");
            }
        } catch (Exception e) {
            log.warn("MCP 解析行失败: {} — {}", line, e.getMessage());
        }
    }
}
