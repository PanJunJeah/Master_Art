package com.pan.master_art.agent.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单次对话（turn）的 Agent 状态：
 * - 通过 ToolContext 传给各工具（跨线程安全，与 Spring AI 原生机制协同）；
 * - 工具执行完毕后回填工具调用轨迹与产物（草稿ID、订阅结果）；
 * - 编排层据此组装返回给前端的结构化响应（草稿卡片等）。
 */
public class AgentTurnState {

    /** 在 ToolContext map 中的键 */
    public static final String TOOL_CONTEXT_KEY = "agentTurnState";

    /** 当前会话用户ID（来自前端，工具必须校验一致） */
    private final Long userId;

    /** 本轮已执行的工具调用步数（步数限制守卫） */
    private final AtomicInteger toolSteps = new AtomicInteger(0);

    /** 本轮新生成的预约草稿ID（用于前端渲染确认卡片） */
    private final List<Long> createdDraftIds = new ArrayList<>();

    /** 本轮订阅操作的结果消息 */
    private final List<String> subscriptionMessages = new ArrayList<>();

    /** 工具调用轨迹（名称/成败/摘要），前端可展示"AI 用了哪些工具" */
    private final List<ToolTrace> toolTraces = new ArrayList<>();

    public AgentTurnState(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    /** 记录一次工具调用并返回当前步数（从 1 开始） */
    public int recordToolInvocation() {
        return toolSteps.incrementAndGet();
    }

    public int getToolSteps() {
        return toolSteps.get();
    }

    public void addDraftId(Long draftId) {
        synchronized (createdDraftIds) {
            createdDraftIds.add(draftId);
        }
    }

    public List<Long> getCreatedDraftIds() {
        synchronized (createdDraftIds) {
            return new ArrayList<>(createdDraftIds);
        }
    }

    public void addSubscriptionMessage(String message) {
        synchronized (subscriptionMessages) {
            subscriptionMessages.add(message);
        }
    }

    public List<String> getSubscriptionMessages() {
        synchronized (subscriptionMessages) {
            return new ArrayList<>(subscriptionMessages);
        }
    }

    public void recordTrace(String name, boolean success, String message) {
        synchronized (toolTraces) {
            toolTraces.add(new ToolTrace(name, success, message));
        }
    }

    public List<ToolTrace> getToolTraces() {
        synchronized (toolTraces) {
            return new ArrayList<>(toolTraces);
        }
    }

    /**
     * 工具调用轨迹
     */
    public static class ToolTrace {
        public final String name;
        public final boolean success;
        public final String message;

        public ToolTrace(String name, boolean success, String message) {
            this.name = name;
            this.success = success;
            this.message = message;
        }
    }
}
