package com.pan.master_art.agent.dto;

import lombok.Data;

/**
 * Agent 聊天请求（前端 → /api/agent/chat）
 */
@Data
public class AgentChatRequest {
    /** 用户消息 */
    private String message;
    /** 当前登录用户ID（可选，未登录为 null；写操作工具要求必须登录） */
    private Long userId;
}
