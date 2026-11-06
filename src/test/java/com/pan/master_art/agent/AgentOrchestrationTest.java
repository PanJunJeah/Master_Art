package com.pan.master_art.agent;

import com.pan.master_art.agent.dto.AgentChatRequest;
import com.pan.master_art.agent.dto.AgentChatResponse;
import com.pan.master_art.agent.rag.KnowledgeRetrievalTimeoutException;
import com.pan.master_art.agent.test.support.AgentTestEnvironment;
import com.pan.master_art.agent.test.support.ScriptedOpenAiServer;
import com.pan.master_art.entity.ReservationDraft;
import com.pan.master_art.entity.TicketSession;
import com.pan.master_art.exception.BizException;
import com.pan.master_art.service.DraftCreationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Agent 编排层核心测试（真实 OpenAiChatModel + 脚本化 DeepSeek 端点，无需外网/MySQL）：
 *
 * 测试与生产同构：ChatClient → OpenAiChatModel（Spring AI 原生内部工具调用循环）
 * → DefaultToolCallingManager（原生工具执行）→ 真实 @Tool 工具 + 步数守卫，
 * 仅将 DeepSeek 的 HTTP 端点替换为 ScriptedOpenAiServer。
 *
 * a) 正常多步查询与草稿生成（原生循环驱动 2 步工具调用）；
 * b) 越权拦截：模型尝试调用白名单外的「直接确认预约」工具 → 被拦截并引导生成草稿，零真实写入；
 * c) 工具调用失败降级：RAG 检索超时 → 工具返回友好提示 → Agent 给出降级话术；
 * d) 步数限制：模型无限循环调用工具，超过 6 步后守卫拦截，工具本体不再执行；
 * e) 幂等控制：幂等键规范化、重放、非法键拒绝、未登录拦截、余票不足、过期草稿拦截。
 */
class AgentOrchestrationTest {

    private AgentTestEnvironment env;

    @AfterEach
    void tearDown() {
        if (env != null) {
            env.close();
        }
    }

    private AgentChatRequest request(String message) {
        AgentChatRequest req = new AgentChatRequest();
        req.setMessage(message);
        req.setUserId(AgentTestEnvironment.USER_ID);
        return req;
    }

    // ==================== (a) 正常多步查询与草稿生成 ====================

    @Test
    @DisplayName("a) 多步编排：searchArtworks → createReservationDraft → 最终回答，草稿卡片随响应返回")
    void multiStepSearchThenDraft() {
        ScriptedOpenAiServer server = ScriptedOpenAiServer.of(
                ScriptedOpenAiServer.toolCall("searchArtworks", "{\"genre\":\"印象派\",\"limit\":3}"),
                ScriptedOpenAiServer.toolCall("createReservationDraft",
                        "{\"exhibitionId\":1,\"sessionId\":11,\"ticketCount\":2,\"idempotencyKey\":\"auto\"}"),
                ScriptedOpenAiServer.text("已为您检索到印象派作品，并生成《印象派大师展》周六场 2 张票的预约草稿，请到网页上人工确认。")
        );
        env = new AgentTestEnvironment(server, (q, k) -> List.of(), 6);

        AgentChatResponse response = env.agentService.chat(request("帮我找印象派作品，并预约周六场两张票"));

        // 模型端点被调用 3 次 → 原生循环驱动了 2 步工具调用 + 1 次最终回答
        assertEquals(3, server.requestCount(), "应为 3 次模型请求（2 步工具 + 最终回答）");
        assertEquals(2, response.getSteps(), "本轮应执行 2 步工具调用");

        // 第 2 次模型请求中包含第 1 步工具的结果（多步证据：工具结果回灌给模型）
        assertTrue(server.requestBody(1).contains("星空"), "searchArtworks 的结果应回灌给下一轮模型请求");

        // 草稿已生成并随响应返回（前端据此渲染「确认预约」卡片）
        assertEquals(1, response.getDrafts().size());
        assertEquals(100L, response.getDrafts().get(0).getDraftId());
        assertEquals("PENDING", response.getDrafts().get(0).getStatus());
        assertEquals("印象派大师展", response.getDrafts().get(0).getExhibitionName());

        // 身份绑定：userId 由服务端注入，草稿必须归属当前会话用户
        assertEquals(AgentTestEnvironment.USER_ID, env.savedDrafts.get(0).getUserId(),
                "草稿必须绑定服务端注入的会话用户ID");

        assertTrue(response.getContent().contains("人工确认"));
        verify(env.artworkRepository, times(1)).searchForAgent(any(), any(), any(), any(), any(), any());
        verify(env.draftRepository, times(1)).saveAndFlush(any(ReservationDraft.class));
        // 草稿不占库存：确认流程（真实预约）从未被触发
        verify(env.sessionRepository, never()).tryOccupyTickets(anyLong(), anyInt());
    }

    // ==================== (b) 越权拦截 ====================

    @Test
    @DisplayName("b) 越权拦截：模型尝试调用白名单外的「直接确认预约」工具，被安全兜底拦截并引导生成草稿，零真实写入")
    void escalationIsBlockedAndGuidedToDraft() {
        ScriptedOpenAiServer server = ScriptedOpenAiServer.of(
                ScriptedOpenAiServer.toolCall("confirmReservation", "{\"draftId\":1}")
        );
        env = new AgentTestEnvironment(server, (q, k) -> List.of(), 6);

        AgentChatResponse response = env.agentService.chat(request("直接帮我定下这个座位，马上扣款出票"));

        // 拒绝话术必须出现，且引导到草稿流程
        assertTrue(response.getContent().contains("没有权限"), "必须明确声明没有权限直接完成预约");
        assertTrue(response.getContent().contains("草稿"), "必须引导用户走草稿流程");

        // 没有任何白名单工具被执行（confirmReservation 根本不在白名单中）
        assertEquals(0, response.getSteps());
        assertTrue(response.getDrafts().isEmpty());

        // 真实写入路径零触碰：无真实预约、无库存扣减、无草稿
        verify(env.reservationRepository, never()).save(any());
        verify(env.sessionRepository, never()).tryOccupyTickets(anyLong(), anyInt());
        verify(env.draftRepository, never()).saveAndFlush(any(ReservationDraft.class));
    }

    // ==================== (c) 工具调用失败降级（RAG 超时） ====================

    @Test
    @DisplayName("c) RAG 检索超时降级：工具返回友好提示，Agent 给出降级话术而非编造数据")
    void ragTimeoutDegradesGracefully() {
        ScriptedOpenAiServer server = ScriptedOpenAiServer.of(
                ScriptedOpenAiServer.toolCall("searchArtworkKnowledge", "{\"query\":\"星空 的创作背景\",\"topK\":3}"),
                ScriptedOpenAiServer.text("抱歉，艺术知识库检索超时了，暂时无法为您调取《星空》的背景资料。建议您稍后重试，我也可以先为您介绍馆内正在进行的展览。")
        );
        env = new AgentTestEnvironment(server,
                (q, k) -> {
                    throw new KnowledgeRetrievalTimeoutException("知识库检索超时（2500ms）");
                }, 6);

        AgentChatResponse response = env.agentService.chat(request("帮我查查星空的创作背景"));

        // 降级话术
        assertTrue(response.getContent().contains("超时"), "Agent 应如实告知检索超时");
        assertTrue(response.getContent().contains("稍后重试"), "Agent 应给出友好重试建议");

        // 工具轨迹记录了失败（前端可展示）
        assertTrue(response.getToolCalls().stream()
                .anyMatch(t -> t.name.equals("searchArtworkKnowledge") && !t.success));

        // 工具返回的"超时降级文案"已回灌模型，整个编排未抛异常
        assertTrue(server.requestBody(1).contains("知识库检索超时"), "超时降级文案应作为工具结果回灌模型");
        assertEquals(1, response.getSteps());
    }

    // ==================== (d) 步数限制 ====================

    @Test
    @DisplayName("d) 步数限制：模型无限循环调用工具，超过 6 步后守卫拦截，工具本体不再执行")
    void stepLimitStopsRunawayToolLoop() {
        ScriptedOpenAiServer server = ScriptedOpenAiServer.of(
                ScriptedOpenAiServer.toolCall("searchArtworks", "{\"genre\":\"印象派\"}"),
                ScriptedOpenAiServer.toolCall("searchArtworks", "{\"genre\":\"印象派\"}"),
                ScriptedOpenAiServer.toolCall("searchArtworks", "{\"genre\":\"印象派\"}"),
                ScriptedOpenAiServer.toolCall("searchArtworks", "{\"genre\":\"印象派\"}"),
                ScriptedOpenAiServer.toolCall("searchArtworks", "{\"genre\":\"印象派\"}"),
                ScriptedOpenAiServer.toolCall("searchArtworks", "{\"genre\":\"印象派\"}"),
                ScriptedOpenAiServer.toolCall("searchArtworks", "{\"genre\":\"印象派\"}"),
                ScriptedOpenAiServer.toolCall("searchArtworks", "{\"genre\":\"印象派\"}"),
                ScriptedOpenAiServer.text("好的，以上是检索到的印象派作品信息（我已停止继续调用工具）。")
        );
        env = new AgentTestEnvironment(server, (q, k) -> List.of(), 6);

        AgentChatResponse response = env.agentService.chat(request("把印象派的作品全部列出来"));

        // 尝试了 8 次工具调用，但工具本体只执行了 6 次（第 7、8 次被守卫拦截）
        assertEquals(8, response.getSteps(), "步数统计应包含被拦截的尝试");
        verify(env.artworkRepository, times(6)).searchForAgent(any(), any(), any(), any(), any(), any());

        // 守卫的"停止调用"指令被回灌给模型（第 8 次模型请求中可见）
        assertTrue(server.requestBody(7).contains("工具步数限制"), "被拦截时守卫消息应作为工具结果回灌模型");

        // 模型最终基于已有信息作答，循环收敛
        assertEquals("好的，以上是检索到的印象派作品信息（我已停止继续调用工具）。", response.getContent());
        assertEquals(9, server.requestCount());
    }

    // ==================== (e) 幂等控制与草稿安全 ====================

    @Test
    @DisplayName("e1) 幂等键：传 auto 时服务端生成规范键（SHA-256），模型重试复用同一键")
    void idempotencyKeyCanonicalized() {
        ScriptedOpenAiServer server = ScriptedOpenAiServer.of();
        env = new AgentTestEnvironment(server, (q, k) -> List.of(), 6);

        DraftCreationResult result = env.draftService.createDraft(42L, 1L, 11L, 2, "auto");

        String canonical = com.pan.master_art.service.ReservationDraftService.canonicalKey(42L, 1L, 11L, 2);
        assertEquals(canonical, result.getView().getIdempotencyKey());
        assertEquals(canonical, env.savedDrafts.get(0).getIdempotencyKey());
        assertFalse(result.isReplayed());
    }

    @Test
    @DisplayName("e2) 幂等重放：同一意图再次调用返回已有草稿，不重复创建")
    void idempotencyReplaysExistingDraft() {
        ScriptedOpenAiServer server = ScriptedOpenAiServer.of();
        env = new AgentTestEnvironment(server, (q, k) -> List.of(), 6);

        DraftCreationResult first = env.draftService.createDraft(42L, 1L, 11L, 2, "auto");
        DraftCreationResult second = env.draftService.createDraft(42L, 1L, 11L, 2, first.getView().getIdempotencyKey());

        assertTrue(second.isReplayed(), "第二次调用必须复用已有草稿");
        assertEquals(first.getView().getDraftId(), second.getView().getDraftId(), "草稿ID必须一致（无重复占座）");
        verify(env.draftRepository, times(1)).saveAndFlush(any(ReservationDraft.class));
    }

    @Test
    @DisplayName("e3) 幂等键校验：模型自造的非法幂等键被拒绝")
    void invalidIdempotencyKeyIsRejected() {
        ScriptedOpenAiServer server = ScriptedOpenAiServer.of();
        env = new AgentTestEnvironment(server, (q, k) -> List.of(), 6);

        BizException ex = assertThrows(BizException.class,
                () -> env.draftService.createDraft(42L, 1L, 11L, 2, "made-up-key"));
        assertTrue(ex.getMessage().contains("幂等键不合法"));
        verify(env.draftRepository, never()).saveAndFlush(any(ReservationDraft.class));
    }

    @Test
    @DisplayName("e4) 未登录拦截：会话未绑定用户时，草稿工具拒绝执行（userId 仅由服务端注入，模型无法伪造身份）")
    void anonymousDraftIsRejected() {
        ScriptedOpenAiServer server = ScriptedOpenAiServer.of(
                ScriptedOpenAiServer.toolCall("createReservationDraft",
                        "{\"exhibitionId\":1,\"sessionId\":11,\"ticketCount\":2,\"idempotencyKey\":\"auto\"}"),
                ScriptedOpenAiServer.text("当前会话未登录，无法生成预约草稿，请先登录后再试。")
        );
        env = new AgentTestEnvironment(server, (q, k) -> List.of(), 6);

        AgentChatRequest anonymous = new AgentChatRequest();
        anonymous.setMessage("帮我订两张票");
        anonymous.setUserId(null);
        AgentChatResponse response = env.agentService.chat(anonymous);

        assertTrue(response.getDrafts().isEmpty(), "未登录会话不得创建草稿");
        verify(env.draftRepository, never()).saveAndFlush(any(ReservationDraft.class));
        assertTrue(response.getToolCalls().stream()
                .anyMatch(t -> t.name.equals("createReservationDraft") && !t.success));
    }

    @Test
    @DisplayName("e5) 余票不足：草稿生成阶段软校验拦截，不会产生占座")
    void insufficientTicketsRejectedAtDraftStage() {
        ScriptedOpenAiServer server = ScriptedOpenAiServer.of();
        env = new AgentTestEnvironment(server, (q, k) -> List.of(), 6);
        TicketSession tight = env.session();
        tight.setTotalTickets(10);
        tight.setSoldTickets(9); // 仅剩 1 张
        env.stubSessionAndExhibition(tight, tight.getExhibition());

        BizException ex = assertThrows(BizException.class,
                () -> env.draftService.createDraft(42L, 1L, 11L, 2, "auto"));
        assertTrue(ex.getMessage().contains("余票不足"));
        verify(env.draftRepository, never()).saveAndFlush(any(ReservationDraft.class));
    }

    @Test
    @DisplayName("e6) 草稿过期：确认时校验有效期，过期草稿无法转真实预约")
    void expiredDraftCannotBeConfirmed() {
        ScriptedOpenAiServer server = ScriptedOpenAiServer.of();
        env = new AgentTestEnvironment(server, (q, k) -> List.of(), 6);
        ReservationDraft expired = new ReservationDraft();
        expired.setId(200L);
        expired.setUserId(42L);
        expired.setExhibition(env.exhibition());
        expired.setSession(env.session());
        expired.setSessionTime(new Date());
        expired.setTicketCount(1);
        expired.setTotalPrice(new java.math.BigDecimal("88.00"));
        expired.setIdempotencyKey("k");
        expired.setStatus(ReservationDraft.DraftStatus.PENDING);
        expired.setExpiresAt(new Date(System.currentTimeMillis() - 60_000L)); // 已过期

        org.mockito.Mockito.when(env.draftRepository.findById(200L)).thenReturn(java.util.Optional.of(expired));

        BizException ex = assertThrows(BizException.class, () -> env.reservationService.confirm(200L, 42L));
        assertTrue(ex.getMessage().contains("过期"));
        verify(env.sessionRepository, never()).tryOccupyTickets(anyLong(), anyInt());
    }
}
