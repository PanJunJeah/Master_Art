package com.pan.master_art.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 真实预约实体类 —— 仅在"人工确认闸门"之后产生
 *
 * 写入路径（唯一）：
 *   Agent 工具 createReservationDraft  →  ReservationDraft（草稿）
 *   Vue3 前端人工点击确认               →  POST /api/reservations/confirm
 *                                      →  ReservationService.confirm()
 *                                      →  条件扣减 TicketSession.soldTickets + 生成 Reservation
 * Agent 全程无法直接调用该接口/服务（工具白名单内不存在确认类工具）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 预约人ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 预约的展览
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Exhibition exhibition;

    /**
     * 预约的场次
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private TicketSession session;

    /**
     * 来源草稿（一对一，一个草稿只能确认一次）
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id", nullable = false, unique = true)
    private ReservationDraft draft;

    /**
     * 预约票数
     */
    @Column(name = "ticket_count", nullable = false)
    private Integer ticketCount = 1;

    /**
     * 订单总价
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    /**
     * 预约状态：CONFIRMED-已确认，CANCELLED-已取消，USED-已核销
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

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

    public enum ReservationStatus {
        CONFIRMED, CANCELLED, USED
    }
}
