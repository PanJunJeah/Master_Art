package com.pan.master_art.agent.orchestration;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * 步数限制守卫（ToolCallback 装饰器）：
 * 在 Spring AI 原生 ToolCalling 循环之外包一层计数器，
 * 单次对话的工具执行步数超过上限（默认 6）后直接返回"停止调用"指令，
 * 工具本体不再执行，从而防止模型陷入无限工具调用死循环。
 *
 * 循环本身仍由 Spring AI 的 ChatClient/DefaultToolCallingManager 驱动（原生 ToolCalling），
 * 本装饰器只负责"限额"，不解析任何 JSON、不接管循环。
 */
public class StepLimitedToolCallback implements ToolCallback {

    /** 超限时返回给模型的提示（会被当作该次工具调用的结果） */
    public static final String STEP_LIMIT_MESSAGE_TEMPLATE =
            "【工具步数限制】已达到单次对话最大工具调用步数(%d)，请立即停止调用工具，基于已有信息直接给出最终回答。";

    private final ToolCallback delegate;
    private final int maxSteps;

    public StepLimitedToolCallback(ToolCallback delegate, int maxSteps) {
        this.delegate = delegate;
        this.maxSteps = Math.max(1, maxSteps);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        AgentTurnState state = stateOf(toolContext);
        if (state != null && state.recordToolInvocation() > maxSteps) {
            return String.format(STEP_LIMIT_MESSAGE_TEMPLATE, maxSteps);
        }
        return delegate.call(toolInput, toolContext);
    }

    private AgentTurnState stateOf(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object state = toolContext.getContext().get(AgentTurnState.TOOL_CONTEXT_KEY);
        return state instanceof AgentTurnState ? (AgentTurnState) state : null;
    }
}
