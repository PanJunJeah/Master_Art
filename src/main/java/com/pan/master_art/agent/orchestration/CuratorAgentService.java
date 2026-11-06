package com.pan.master_art.agent.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pan.master_art.agent.dto.AgentChatRequest;
import com.pan.master_art.agent.dto.AgentChatResponse;
import com.pan.master_art.dto.DraftView;
import com.pan.master_art.service.ReservationDraftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能策展 Agent 编排层
 *
 * 1. 多步工具调用循环由 Spring AI ChatClient 原生 ToolCalling 机制驱动（不手写 JSON 解析循环）；
 * 2. 步数限制由 StepLimitedToolCallback 装饰器在工具执行层强制（默认 6 步）；
 * 3. 每轮对话通过 ToolContext 注入 AgentTurnState，收集草稿/订阅/工具轨迹，
 *    把"结构化产物"（预约草稿卡片）随回答一起返回给前端；
 * 4. 对话记忆按 userId 隔离（MessageWindowChatMemory + MessageChatMemoryAdvisor）。
 */
@Service
public class CuratorAgentService {

    private static final Logger log = LoggerFactory.getLogger(CuratorAgentService.class);

    private final ChatClient curatorChatClient;
    private final ChatMemory chatMemory;
    private final ReservationDraftService draftService;
    private final ObjectMapper objectMapper;

    public CuratorAgentService(ChatClient curatorChatClient,
                               ChatMemory chatMemory,
                               ReservationDraftService draftService,
                               ObjectMapper objectMapper) {
        this.curatorChatClient = curatorChatClient;
        this.chatMemory = chatMemory;
        this.draftService = draftService;
        this.objectMapper = objectMapper;
    }

    /**
     * 非流式对话（JSON 返回，含结构化产物）
     */
    public AgentChatResponse chat(AgentChatRequest request) {
        AgentTurnState state = new AgentTurnState(request.getUserId());
        String conversationId = conversationIdOf(request.getUserId());

        String content;
        try {
            content = curatorChatClient.prompt()
                    .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                            .conversationId(conversationId)
                            .build())
                    .toolContext(Map.of(AgentTurnState.TOOL_CONTEXT_KEY, state))
                    .user(request.getMessage())
                    .call()
                    .content();
        } catch (Exception e) {
            // 安全兜底：工具白名单外的调用（如模型尝试"直接确认预约"）或模型服务异常，
            // 一律收敛为有界的拒绝话术，绝不产生任何真实写入。
            log.error("Agent 对话执行失败（安全兜底收敛）", e);
            content = "抱歉，我刚才的请求被安全策略拦截或服务暂时不可用："
                    + "我没有权限直接完成预约、锁座或扣款，只能为您生成预约草稿。"
                    + "如果您想预约，请让我生成草稿，然后由您本人到网页上点击「确认预约」完成。";
        }

        return assemble(state, content);
    }

    /**
     * 流式对话（SSE）：
     * - event "token"：回答片段；
     * - event "done"：最终结构化响应 JSON（含草稿卡片等产物）；
     * - event "error"：异常时的友好提示。
     */
    public Flux<ServerSentEvent<String>> chatStreamSse(AgentChatRequest request) {
        AgentTurnState state = new AgentTurnState(request.getUserId());
        String conversationId = conversationIdOf(request.getUserId());
        StringBuilder contentBuilder = new StringBuilder();

        Flux<String> tokens = curatorChatClient.prompt()
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .toolContext(Map.of(AgentTurnState.TOOL_CONTEXT_KEY, state))
                .user(request.getMessage())
                .stream()
                .content();

        return tokens
                .doOnNext(contentBuilder::append)
                .map(token -> ServerSentEvent.<String>builder()
                        .event("token")
                        .data(token)
                        .build())
                .concatWith(Mono.fromCallable(() -> {
                    AgentChatResponse response = assemble(state, contentBuilder.toString());
                    return ServerSentEvent.<String>builder()
                            .event("done")
                            .data(objectMapper.writeValueAsString(response))
                            .build();
                }))
                .onErrorResume(e -> {
                    log.error("Agent 流式对话异常", e);
                    return Mono.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("{\"message\":\"AI 服务被安全策略拦截或暂时不可用：我没有权限直接完成预约或扣款，只能生成预约草稿，请稍后重试\"}")
                            .build());
                });
    }

    /**
     * 组装最终结构化响应：草稿卡片 + 订阅消息 + 工具轨迹
     */
    private AgentChatResponse assemble(AgentTurnState state, String content) {
        List<DraftView> drafts = new ArrayList<>();
        for (Long draftId : state.getCreatedDraftIds()) {
            try {
                DraftView view = draftService.getView(draftId);
                if (view != null) {
                    drafts.add(view);
                }
            } catch (Exception e) {
                log.warn("读取草稿视图失败 draftId={}", draftId, e);
            }
        }
        AgentChatResponse response = new AgentChatResponse();
        response.setContent(content);
        response.setDrafts(drafts);
        response.setSubscriptions(state.getSubscriptionMessages());
        response.setToolCalls(state.getToolTraces());
        response.setSteps(state.getToolSteps());
        return response;
    }

    /**
     * 清空当前用户的对话记忆（前端「新对话」按钮）
     */
    public void clearMemory(Long userId) {
        chatMemory.clear(conversationIdOf(userId));
        log.info("已清空用户对话记忆 userId={}", userId);
    }

    private String conversationIdOf(Long userId) {
        return userId == null ? "anonymous" : "user-" + userId;
    }
}
