package com.pan.master_art.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 新展订阅实体类
 * Agent 受限写工具 createSubscription 可为用户登记新展提醒订阅。
 * 幂等：同一用户对同一展览只能存在一条有效订阅（唯一约束 uk_subscription_user_exhibition）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exhibition_subscriptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_subscription_user_exhibition", columnNames = {"user_id", "exhibition_id"}))
public class ExhibitionSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Exhibition exhibition;

    /**
     * 状态：1-订阅中，0-已取消
     */
    @Column(nullable = false)
    private Integer status = 1;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private Date subscribedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        subscribedAt = new Date();
        updateTime = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
