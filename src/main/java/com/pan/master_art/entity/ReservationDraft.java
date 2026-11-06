package com.pan.master_art.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预约草稿实体类 —— Agent 编排层的"人工确认闸门"核心
 *
 * Agent（大模型）通过受限工具 createReservationDraft 只能生成草稿，
 * 永远不能直接扣减库存 / 锁座 / 完成预约。
 * 用户在前端确认草稿后，由 Vue3 直接调用真实预约接口 POST /api/reservations/confirm 完成写入。
 *
 * 幂等保障（双层）：
 * 1. 应用层：idempotency_key = SHA-256(userId|exhibitionId|sessionId|ticketCount)，
 *    同一预约意图重试时返回已有草稿，不重复创建；
 * 2. 数据库层：idempotency_key 唯一约束 uk_draft_idempotency 兜底，
 *    并发重试触发唯一键冲突时降级为查询已有草稿。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservation_drafts",
        uniqueConstraints = @UniqueConstraint(name = "uk_draft_idempotency", columnNames = "idempotency_key"))
public class ReservationDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID（从前端会话/JWT 传入，Agent 工具参数必须与之一致）
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 目标展览
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Exhibition exhibition;

    /**
     * 目标场次（排期）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private TicketSession session;

    /**
     * 场次时间快照（草稿生成时的场次时间，避免后续排期变化影响展示）
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "session_time", nullable = false)
    private Date sessionTime;

    /**
     * 预约票数
     */
    @Column(name = "ticket_count", nullable = false)
    private Integer ticketCount = 1;

    /**
     * 订单总价 = 单价 × 票数（草稿生成时锁定）
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    /**
     * 幂等键（唯一约束），由工具层强制要求传入
     */
    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    /**
     * 草稿状态：PENDING-待确认，CONFIRMED-已确认（已生成真实预约），EXPIRED-已过期，CANCELLED-已取消
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DraftStatus status = DraftStatus.PENDING;

    /**
     * 草稿过期时间（默认 30 分钟，过期未确认自动失效）
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }

    public enum DraftStatus {
        PENDING,    // 待人工确认
        CONFIRMED,  // 已确认，已转为真实预约
        EXPIRED,    // 超时未确认，自动过期
        CANCELLED   // 用户取消
    }
}
