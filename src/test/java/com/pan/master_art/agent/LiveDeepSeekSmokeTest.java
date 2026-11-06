package com.pan.master_art.agent;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实 DeepSeek 连通性冒烟测试（可选运行，默认跳过）：
 * 1. 纯聊天链路：Spring AI → DeepSeek；
 * 2. 函数调用链路：DeepSeek 通过 OpenAI 兼容协议调用 @Tool 工具，
 *    验证生产环境原生 ToolCalling 协议可用。
 *
 * 运行方式：
 *   mvn test -Dtest=LiveDeepSeekSmokeTest -Dagent.live=true -Dai.api.key=sk-xxx
 * 或设置环境变量 AI_API_KEY。
 */
@Tag("live")
class LiveDeepSeekSmokeTest {

    /** 演示用只读工具（不依赖数据库） */
    public static class MuseumInfoTool {
        @Tool(name = "getMuseumName", description = "查询美术馆的名称")
        public String getMuseumName() {
            return "MasterArt 美术馆";
        }
    }

    private String apiKey() {
        String key = System.getenv("AI_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getProperty("ai.api.key");
        }
        return key;
    }

    private void assumeLive() {
        Assumptions.assumeTrue(System.getProperty("agent.live", "false").equalsIgnoreCase("true"),
                "live 测试默认跳过（-Dagent.live=true 启用）");
        Assumptions.assumeTrue(apiKey() != null && !apiKey().isBlank(), "未配置 AI_API_KEY / -Dai.api.key，跳过");
    }

    private OpenAiChatModel newModel() {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-chat")
                        .temperature(0.2)
                        .build())
                .build();
    }

    @Test
    @DisplayName("live-1) 通过 Spring AI 调用真实 DeepSeek（纯聊天）")
    void deepSeekChatThroughSpringAi() {
        assumeLive();
        ChatClient client = ChatClient.builder(newModel())
                .defaultSystem("你是美术馆导览助手，只允许使用中文，用一句话回答。")
                .build();

        String answer = client.prompt().user("请一句话介绍你们美术馆").call().content();
        assertNotNull(answer, "DeepSeek 应返回内容");
        assertFalse(answer.isBlank(), "DeepSeek 返回内容不应为空");
    }

    @Test
    @DisplayName("live-2) DeepSeek 原生函数调用：模型调用 @Tool 工具后作答")
    void deepSeekToolCallingRoundTrip() {
        assumeLive();
        ToolCallback callback = MethodToolCallbackProvider.builder()
                .toolObjects(new MuseumInfoTool())
                .build()
                .getToolCallbacks()[0];

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl("https://api.deepseek.com")
                        .apiKey(apiKey())
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-chat")
                        .temperature(0.2)
                        .toolCallbacks(callback)
                        .internalToolExecutionEnabled(true)
                        .build())
                .toolCallingManager(DefaultToolCallingManager.builder().build())
                .build();

        ChatClient client = ChatClient.builder(model)
                .defaultSystem("你是美术馆导览助手，只允许使用中文。")
                .build();

        String answer = client.prompt()
                .user("请调用工具查询你们美术馆的名称，然后告诉我全名")
                .call()
                .content();
        assertNotNull(answer, "DeepSeek 应返回内容");
        assertTrue(answer.contains("MasterArt"),
                "DeepSeek 应通过工具调用获得真实名称，实际回答：" + answer);
    }
}
