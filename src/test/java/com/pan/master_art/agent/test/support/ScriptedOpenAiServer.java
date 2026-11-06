package com.pan.master_art.agent.test.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 脚本化 OpenAI 兼容 HTTP 服务（测试替身，替代真实 DeepSeek 端点）：
 * 按脚本依次返回 chat.completion 响应（工具调用 / 最终文本），
 * 配合真实 OpenAiChatModel 驱动 Spring AI 原生内部工具调用循环，
 * 让测试走"与生产完全一致"的代码路径（ChatClient → OpenAiChatModel → 原生 ToolCalling）。
 */
public class ScriptedOpenAiServer implements AutoCloseable {

    private static final ObjectMapper OM = new ObjectMapper();

    /** 每次请求的原始 JSON 请求体（用于断言工具结果回灌等多步证据） */
    public final List<String> requestBodies = new ArrayList<>();

    private final Deque<String> script = new ArrayDeque<>();
    private final HttpServer server;

    private ScriptedOpenAiServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/chat/completions", this::handle);
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException("无法启动脚本化 HTTP 服务", e);
        }
    }

    public static ScriptedOpenAiServer of(String... scriptedResponseJson) {
        ScriptedOpenAiServer server = new ScriptedOpenAiServer();
        for (String json : scriptedResponseJson) {
            server.script.addLast(json);
        }
        return server;
    }

    /** 构造"要求调用某工具"的 OpenAI 响应 */
    public static String toolCall(String toolName, String argumentsJson) {
        ObjectNode function = OM.createObjectNode();
        function.put("name", toolName);
        function.put("arguments", argumentsJson);
        ObjectNode toolCallNode = OM.createObjectNode();
        toolCallNode.put("id", "call_" + toolName + "_" + System.nanoTime());
        toolCallNode.put("type", "function");
        toolCallNode.set("function", function);
        ArrayNode toolCalls = OM.createArrayNode();
        toolCalls.add(toolCallNode);
        ObjectNode message = OM.createObjectNode();
        message.put("role", "assistant");
        message.putNull("content");
        message.set("tool_calls", toolCalls);
        return completionJson(message, "tool_calls");
    }

    /** 构造纯文本最终响应（循环终点） */
    public static String text(String content) {
        ObjectNode message = OM.createObjectNode();
        message.put("role", "assistant");
        message.put("content", content);
        return completionJson(message, "stop");
    }

    private static String completionJson(ObjectNode message, String finishReason) {
        ObjectNode choice = OM.createObjectNode();
        choice.put("index", 0);
        choice.set("message", message);
        choice.put("finish_reason", finishReason);
        ArrayNode choices = OM.createArrayNode();
        choices.add(choice);
        ObjectNode root = OM.createObjectNode();
        root.put("id", "chatcmpl-scripted");
        root.put("object", "chat.completion");
        root.put("created", 1730000000L);
        root.put("model", "deepseek-chat");
        root.set("choices", choices);
        return root.toString();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            synchronized (requestBodies) {
                requestBodies.add(body);
            }
            String response = script.pollFirst();
            if (response == null) {
                byte[] err = "{\"error\":{\"message\":\"script exhausted\"}}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, err.length);
                exchange.getResponseBody().write(err);
            } else {
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        } finally {
            exchange.close();
        }
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public int requestCount() {
        synchronized (requestBodies) {
            return requestBodies.size();
        }
    }

    public String requestBody(int index) {
        synchronized (requestBodies) {
            return requestBodies.get(index);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
