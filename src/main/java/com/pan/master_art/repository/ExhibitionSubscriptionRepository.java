package com.pan.master_art.repository;

import com.pan.master_art.entity.ExhibitionSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 新展订阅数据访问层
 */
@Repository
public interface ExhibitionSubscriptionRepository extends JpaRepository<ExhibitionSubscription, Long> {

    Optional<ExhibitionSubscription> findByUserIdAndExhibitionId(Long userId, Long exhibitionId);

    List<ExhibitionSubscription> findByUserIdAndStatusOrderBySubscribedAtDesc(Long userId, Integer status);
}
