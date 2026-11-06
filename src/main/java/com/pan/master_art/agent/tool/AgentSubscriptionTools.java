package com.pan.master_art.agent.tool;

import com.pan.master_art.agent.orchestration.AgentTurnState;
import com.pan.master_art.exception.BizException;
import com.pan.master_art.service.SubscriptionService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 新展订阅工具（受限写操作 —— Agent 工具白名单成员）
 * 仅登记"开展/开票提醒"，不涉及票务、座位与款项；幂等（同用户同展览仅一条有效订阅）。
 */
@Component
public class AgentSubscriptionTools {

    private final SubscriptionService subscriptionService;

    public AgentSubscriptionTools(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Tool(name = "createSubscription",
            description = "为用户登记新展提醒订阅（受限写操作）。仅登记提醒，不涉及任何票务、座位或款项；" +
                    "订阅自动绑定当前登录用户（userId 由服务端注入）；同一展览重复订阅会被幂等合并。")
    public String createSubscription(
            @ToolParam(required = true, description = "要订阅提醒的展览ID") Long exhibitionId,
            ToolContext toolContext) {
        AgentTurnState state = AgentToolSupport.stateOf(toolContext);
        try {
            if (state == null || state.getUserId() == null) {
                AgentToolSupport.trace(state, "createSubscription", false, "会话未登录");
                return "当前会话未登录，无法订阅。请引导用户先登录后重试。";
            }
            Long userId = state.getUserId();
            String message = subscriptionService.subscribe(userId, exhibitionId);
            state.addSubscriptionMessage(message);
            AgentToolSupport.trace(state, "createSubscription", true, "订阅成功/已存在");
            return message;
        } catch (BizException e) {
            AgentToolSupport.trace(state, "createSubscription", false, e.getMessage());
            return "订阅失败：" + e.getMessage() + "。请向用户说明原因。";
        } catch (Exception e) {
            AgentToolSupport.trace(state, "createSubscription", false, "未知异常");
            return "订阅出现异常：" + e.getMessage() + "。请向用户致歉并建议稍后重试。";
        }
    }
}
