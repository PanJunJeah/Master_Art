package com.pan.master_art.repository;

import com.pan.master_art.entity.ReservationDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 预约草稿数据访问层
 * idempotency_key 唯一约束（uk_draft_idempotency）是幂等控制的数据库兜底。
 */
@Repository
public interface ReservationDraftRepository extends JpaRepository<ReservationDraft, Long> {

    /**
     * 幂等查询：同一用户 + 同一幂等键只允许一条草稿
     */
    Optional<ReservationDraft> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /**
     * 用户草稿列表（新→旧）
     */
    List<ReservationDraft> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 找出已过期但仍处于 PENDING 状态的草稿（定时任务用）
     */
    List<ReservationDraft> findByStatusAndExpiresAtBefore(ReservationDraft.DraftStatus status, Date time);
}
