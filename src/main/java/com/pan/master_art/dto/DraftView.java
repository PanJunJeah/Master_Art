package com.pan.master_art.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 预约草稿视图（返回给 Agent 编排层与前端卡片）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftView {
    /** 草稿ID（前端确认预约时提交） */
    private Long draftId;
    private Long exhibitionId;
    private String exhibitionName;
    private Long sessionId;
    /** 场次时间（格式化字符串） */
    private String sessionTime;
    private Integer ticketCount;
    private BigDecimal totalPrice;
    /** PENDING / CONFIRMED / EXPIRED / CANCELLED */
    private String status;
    /** 过期时间（时间戳） */
    private Date expiresAt;
    /** 幂等键（模型重试时原样复用） */
    private String idempotencyKey;
    /** 是否复用已有草稿（幂等重放） */
    private Boolean replayed;
}
