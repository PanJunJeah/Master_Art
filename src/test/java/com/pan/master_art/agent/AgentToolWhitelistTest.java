package com.pan.master_art.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pan.master_art.agent.config.AgentConfig;
import com.pan.master_art.agent.orchestration.StepLimitedToolCallback;
import com.pan.master_art.agent.tool.AgentArtworkTools;
import com.pan.master_art.agent.tool.AgentExhibitionTools;
import com.pan.master_art.agent.tool.AgentReservationTools;
import com.pan.master_art.agent.tool.AgentSubscriptionTools;
import com.pan.master_art.repository.ArtworkRepository;
import com.pan.master_art.repository.ExhibitionRepository;
import com.pan.master_art.repository.ReservationDraftRepository;
import com.pan.master_art.repository.TicketSessionRepository;
import com.pan.master_art.service.ReservationDraftService;
import com.pan.master_art.service.ReservationService;
import com.pan.master_art.service.SubscriptionService;
import com.pan.master_art.service.TicketSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Agent 工具白名单安全自检（面试重点的自动化防线）：
 * 1. 模型可见的工具恰好是白名单 5 个，多一个少一个都失败；
 * 2. 任何"确认/支付/占座/库存"语义的工具名都不允许出现；
 * 3. 真实写入方法（ReservationService.confirm、TicketSessionRepository.tryOccupyTickets）
 *    在任何情况下都不允许带 @Tool 注解；
 * 4. 所有工具必须包裹 StepLimitedToolCallback 步数守卫。
 */
class AgentToolWhitelistTest {

    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "searchArtworkKnowledge",   // 只读：RAG 知识检索
            "searchArtworks",           // 只读：多条件作品检索
            "queryExhibition",          // 只读：展览排期/余票
            "createReservationDraft",   // 受限写：仅生成草稿
            "createSubscription"        // 受限写：仅登记订阅提醒
    );

    private ToolCallback[] buildProductionCallbacks() {
        ReservationDraftService draftService = new ReservationDraftService(
                mock(ReservationDraftRepository.class), mock(TicketSessionRepository.class),
                mock(ExhibitionRepository.class), 30);
        ObjectMapper om = new ObjectMapper();
        return AgentConfig.buildGuardedToolCallbacks(6,
                new AgentArtworkTools(mock(ArtworkRepository.class), (q, k) -> java.util.List.of(), om),
                new AgentExhibitionTools(mock(ExhibitionRepository.class),
                        new TicketSessionService(mock(TicketSessionRepository.class)), om),
                new AgentReservationTools(draftService, om),
                new AgentSubscriptionTools(new SubscriptionService(
                        mock(com.pan.master_art.repository.ExhibitionSubscriptionRepository.class),
                        mock(ExhibitionRepository.class))));
    }

    @Test
    @DisplayName("白名单精确匹配：模型可见的工具恰好是 5 个授权工具")
    void exposedToolNamesExactlyMatchWhitelist() {
        ToolCallback[] callbacks = buildProductionCallbacks();
        Set<String> actual = Arrays.stream(callbacks)
                .map(cb -> cb.getToolDefinition().name())
                .collect(Collectors.toSet());
        assertEquals(ALLOWED_TOOLS, actual, "暴露给模型(Agent)的工具集合必须与白名单完全一致");
    }

    @Test
    @DisplayName("禁止任何真实写入语义的工具名（确认/支付/占座/扣库存）")
    void forbiddenWriteSemanticsAreAbsent() {
        for (ToolCallback cb : buildProductionCallbacks()) {
            String name = cb.getToolDefinition().name().toLowerCase();
            assertFalse(name.contains("confirm"), "禁止暴露确认类工具：" + name);
            assertFalse(name.contains("pay"), "禁止暴露支付类工具：" + name);
            assertFalse(name.contains("seat"), "禁止暴露锁座类工具：" + name);
            assertFalse(name.contains("occupy"), "禁止暴露占库存类工具：" + name);
            assertFalse(name.contains("stock"), "禁止暴露库存类工具：" + name);
        }
    }

    @Test
    @DisplayName("真实写入路径（确认预约/扣库存）在任何情况下都不带 @Tool 注解")
    void realWritePathNeverAnnotatedAsTool() throws NoSuchMethodException {
        assertFalse(ReservationService.class.getMethod("confirm", Long.class, Long.class)
                        .isAnnotationPresent(Tool.class),
                "真实预约确认方法严禁注册为 Agent 工具（人工确认闸门）");
        assertFalse(TicketSessionRepository.class.getMethod("tryOccupyTickets", Long.class, int.class)
                        .isAnnotationPresent(Tool.class),
                "库存扣减方法严禁注册为 Agent 工具");
        assertFalse(TicketSessionRepository.class.getMethod("releaseTickets", Long.class, int.class)
                        .isAnnotationPresent(Tool.class),
                "库存回补方法严禁注册为 Agent 工具");
    }

    @Test
    @DisplayName("所有工具均包裹步数守卫（StepLimitedToolCallback）")
    void everyToolIsStepGuarded() {
        for (ToolCallback cb : buildProductionCallbacks()) {
            assertTrue(cb instanceof StepLimitedToolCallback,
                    "工具 " + cb.getToolDefinition().name() + " 必须包裹步数守卫");
        }
    }
}
