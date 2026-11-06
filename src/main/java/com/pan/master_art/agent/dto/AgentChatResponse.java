package com.pan.master_art.agent.dto;

import com.pan.master_art.agent.orchestration.AgentTurnState;
import com.pan.master_art.dto.DraftView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent 聊天响应（结构化）：
 * - content：模型最终回答；
 * - drafts：本轮新生成的预约草稿卡片（前端渲染「确认预约」按钮，人工确认后走真实预约接口）；
 * - subscriptions：本轮订阅结果消息；
 * - toolCalls：工具调用轨迹（前端展示"AI 使用了哪些工具"）；
 * - steps：本轮实际工具调用步数（受 agent.max-tool-steps 限制）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponse {
    private String content;
    private List<DraftView> drafts;
    private List<String> subscriptions;
    private List<AgentTurnState.ToolTrace> toolCalls;
    private int steps;
}
