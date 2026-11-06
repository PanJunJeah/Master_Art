package com.pan.master_art.agent.config;

import com.pan.master_art.agent.AgentPrompts;
import com.pan.master_art.agent.orchestration.StepLimitedToolCallback;
import com.pan.master_art.agent.tool.AgentArtworkTools;
import com.pan.master_art.agent.tool.AgentExhibitionTools;
import com.pan.master_art.agent.tool.AgentReservationTools;
import com.pan.master_art.agent.tool.AgentSubscriptionTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Agent 编排层装配：
 * - ChatMemory：按 userId 隔离的滑动窗口记忆；
 * - 工具注册：仅注册白名单内的 5 个 @Tool 工具（3 只读 + 2 受限写），
 *   全部包裹 StepLimitedToolCallback 步数守卫（默认 6 步）；
 * - ChatClient：Spring AI 原生 ToolCalling 循环（内部多步调用由框架驱动）。
 *
 * 安全要点：ReservationService.confirm / TicketSessionRepository.tryOccupyTickets
 * 等真实写入路径【永不】注册为工具，Agent 无法触达。
 */
@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    @Value("${agent.max-tool-steps:6}")
    private int maxToolSteps;

    @Bean
    public ChatMemory curatorChatMemory(@Value("${agent.chat-memory.max-messages:20}") int maxMessages) {
        return MessageWindowChatMemory.builder().maxMessages(maxMessages).build();
    }

    /**
     * 工具白名单（Agent 唯一的后端能力出口）
     */
    @Bean
    public ToolCallbackProvider curatorToolCallbacks(AgentArtworkTools artworkTools,
                                                     AgentExhibitionTools exhibitionTools,
                                                     AgentReservationTools reservationTools,
                                                     AgentSubscriptionTools subscriptionTools) {
        ToolCallback[] guarded = buildGuardedToolCallbacks(maxToolSteps,
                artworkTools, exhibitionTools, reservationTools, subscriptionTools);

        log.info("Agent 工具白名单（{} 个，单轮步数上限 {}）：{}",
                guarded.length, maxToolSteps,
                Arrays.stream(guarded).map(cb -> cb.getToolDefinition().name()).collect(Collectors.toList()));
        return () -> guarded;
    }

    /**
     * 统一构建工具回调并包裹步数守卫。
     * 生产装配与测试（白名单自检/编排测试）共用同一路径，保证"测的就是上线的"。
     */
    public static ToolCallback[] buildGuardedToolCallbacks(int maxSteps, Object... toolObjects) {
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(toolObjects)
                .build()
                .getToolCallbacks();
        return Arrays.stream(callbacks)
                .map(cb -> new StepLimitedToolCallback(cb, maxSteps))
                .toArray(ToolCallback[]::new);
    }

    /**
     * 策展 Agent 的 ChatClient（DeepSeek，OpenAI 兼容协议）
     */
    @Bean
    public ChatClient curatorChatClient(ChatClient.Builder builder, ToolCallbackProvider curatorToolCallbacks) {
        return builder
                .defaultSystem(AgentPrompts.SYSTEM_PROMPT)
                .defaultToolCallbacks(curatorToolCallbacks)
                .build();
    }
}
