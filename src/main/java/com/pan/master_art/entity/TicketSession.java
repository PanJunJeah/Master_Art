package com.pan.master_art.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 展览排期（场次）实体类
 * 一场展览可包含多个时间段场次，每个场次拥有独立票池。
 * Agent 只读工具 queryExhibition 查询排期与余票；
 * 余票扣减只发生在用户人工确认预约草稿之后（真实预约接口内），Agent 永远无法直接触碰。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_sessions")
public class TicketSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属展览
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Exhibition exhibition;

    /**
     * 场次时间（展览入场时段）
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "session_time", nullable = false)
    private Date sessionTime;

    /**
     * 场次总票数
     */
    @Column(name = "total_tickets", nullable = false)
    private Integer totalTickets = 0;

    /**
     * 已售票数（库存扣减唯一入口：ReservationService.confirm 的条件更新）
     */
    @Column(name = "sold_tickets", nullable = false)
    private Integer soldTickets = 0;

    /**
     * 单张票价（元）
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /**
     * 状态：1-售票中，0-停售
     */
    @Column(nullable = false)
    private Integer status = 1;

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

    /**
     * 剩余票数
     */
    public int remainingTickets() {
        return Math.max(0, totalTickets - soldTickets);
    }
}
