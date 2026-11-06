package com.pan.master_art.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pan.master_art.agent.orchestration.AgentTurnState;
import com.pan.master_art.exception.BizException;
import com.pan.master_art.service.DraftCreationResult;
import com.pan.master_art.service.ReservationDraftService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 预约草稿工具（受限写操作 —— Agent 工具白名单成员）
 *
 * 安全边界（面试重点）：
 * 1. 本工具只能生成 ReservationDraft（草稿），不锁座、不扣库存、不扣款；
 * 2. 真实预约由用户在前端看到草稿后人工确认，由 Vue3 直接调用 POST /api/reservations/confirm；
 *    该接口与 ReservationService.confirm 永不注册为工具，模型没有任何路径直接完成预约；
 * 3. 幂等：必须传入幂等键（可传 auto，服务端按当前会话用户+展览+场次+票数生成规范键并回传），
 *    服务端以规范键 + 数据库唯一约束双层防重，模型重试不会重复占座；
 * 4. userId 由服务端从会话上下文（ToolContext）注入，工具签名不接受 userId 参数——模型无法替他人预约。
 */
@Component
public class AgentReservationTools {

    private final ReservationDraftService draftService;
    private final ObjectMapper objectMapper;

    public AgentReservationTools(ReservationDraftService draftService, ObjectMapper objectMapper) {
        this.draftService = draftService;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "createReservationDraft",
            description = "生成预约草稿（受限写操作）。只能生成草稿，不能完成预约、锁座或扣款；" +
                    "用户必须在网页上人工确认草稿后，系统才会完成真实预约。" +
                    "预约自动绑定当前登录用户（userId 由服务端注入，无需也无法指定他人）。" +
                    "同一预约意图重试必须复用同一个幂等键，避免重复生成草稿。")
    public String createReservationDraft(
            @ToolParam(required = true, description = "展览ID（来自 queryExhibition 结果）") Long exhibitionId,
            @ToolParam(required = true, description = "场次ID（来自 queryExhibition 的 sessions）") Long sessionId,
            @ToolParam(required = false, description = "预约票数，默认1，最多6") Integer ticketCount,
            @ToolParam(required = true, description = "幂等键：同一预约意图必须复用同一键；不会计算可传 'auto'，" +
                    "服务端将按当前会话用户+展览+场次+票数生成规范键并回传，重试时请原样复用") String idempotencyKey,
            ToolContext toolContext) {
        AgentTurnState state = AgentToolSupport.stateOf(toolContext);
        try {
            // 安全设计：userId 由服务端从会话上下文注入，模型无法伪造他人身份
            if (state == null || state.getUserId() == null) {
                AgentToolSupport.trace(state, "createReservationDraft", false, "会话未登录");
                return "当前会话未登录，无法生成预约草稿。请引导用户先登录后重试。";
            }
            Long userId = state.getUserId();

            DraftCreationResult result = draftService.createDraft(userId, exhibitionId, sessionId, ticketCount, idempotencyKey);
            if (result.getView() != null) {
                state.addDraftId(result.getView().getDraftId());
            }
            AgentToolSupport.trace(state, "createReservationDraft", true,
                    result.isReplayed() ? "幂等重放草稿" : "生成草稿");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("draftId", result.getView().getDraftId());
            payload.put("exhibitionId", result.getView().getExhibitionId());
            payload.put("exhibitionName", result.getView().getExhibitionName());
            payload.put("sessionTime", result.getView().getSessionTime());
            payload.put("ticketCount", result.getView().getTicketCount());
            payload.put("totalPrice", result.getView().getTotalPrice());
            payload.put("status", result.getView().getStatus());
            payload.put("expiresAt", result.getView().getExpiresAt());
            payload.put("idempotencyKey", result.getView().getIdempotencyKey());
            payload.put("replayed", result.isReplayed());

            String notice = result.isReplayed()
                    ? "检测到相同预约意图的已有草稿，已为您复用（幂等重放），不会重复占座：\n"
                    : "预约草稿已生成（尚未完成预约、未扣款、未锁座）：\n";
            return notice + objectMapper.writeValueAsString(payload)
                    + "\n请明确告知用户：这只是草稿，需要用户本人到网页上确认后才能完成真实预约。";
        } catch (BizException e) {
            AgentToolSupport.trace(state, "createReservationDraft", false, e.getMessage());
            return "生成预约草稿失败：" + e.getMessage() + "。请向用户说明原因。";
        } catch (Exception e) {
            AgentToolSupport.trace(state, "createReservationDraft", false, "未知异常");
            return "生成预约草稿出现异常：" + e.getMessage() + "。请向用户致歉并建议稍后重试。";
        }
    }
}
