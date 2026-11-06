package com.pan.master_art.repository;

import com.pan.master_art.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 真实预约数据访问层（仅人工确认后产生）
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * 草稿与真实预约一对一：确认接口的幂等守卫
     */
    Optional<Reservation> findByDraftId(Long draftId);

    List<Reservation> findByUserIdOrderByCreateTimeDesc(Long userId);
}
