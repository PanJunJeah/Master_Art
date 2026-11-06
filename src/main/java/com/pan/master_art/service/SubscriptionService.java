package com.pan.master_art.service;

import com.pan.master_art.entity.Exhibition;
import com.pan.master_art.entity.ExhibitionSubscription;
import com.pan.master_art.exception.BizException;
import com.pan.master_art.repository.ExhibitionRepository;
import com.pan.master_art.repository.ExhibitionSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 新展订阅服务
 * Agent 受限写工具 createSubscription 与前端订阅按钮共用；
 * 幂等：同一用户对同一展览仅一条有效订阅（唯一约束 uk_subscription_user_exhibition）。
 */
@Service
public class SubscriptionService {

    private final ExhibitionSubscriptionRepository subscriptionRepository;
    private final ExhibitionRepository exhibitionRepository;

    public SubscriptionService(ExhibitionSubscriptionRepository subscriptionRepository,
                               ExhibitionRepository exhibitionRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.exhibitionRepository = exhibitionRepository;
    }

    @Transactional
    public String subscribe(Long userId, Long exhibitionId) {
        if (userId == null || exhibitionId == null) {
            throw new BizException("userId 与 exhibitionId 不能为空");
        }
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new BizException("展览不存在：id=" + exhibitionId));

        Optional<ExhibitionSubscription> existing = subscriptionRepository.findByUserIdAndExhibitionId(userId, exhibitionId);
        if (existing.isPresent()) {
            ExhibitionSubscription sub = existing.get();
            if (sub.getStatus() != null && sub.getStatus() == 1) {
                return "您已订阅《" + exhibition.getName() + "》，无需重复订阅，开展前我们会第一时间通知您。";
            }
            sub.setStatus(1);
            subscriptionRepository.save(sub);
            return "已重新为您开启《" + exhibition.getName() + "》的新展提醒订阅。";
        }

        ExhibitionSubscription sub = new ExhibitionSubscription();
        sub.setUserId(userId);
        sub.setExhibition(exhibition);
        sub.setStatus(1);
        sub.setSubscribedAt(new Date());
        subscriptionRepository.save(sub);
        return "订阅成功！《" + exhibition.getName() + "》开展或开票时我们会通知您。";
    }

    @Transactional
    public String unsubscribe(Long userId, Long exhibitionId) {
        Optional<ExhibitionSubscription> existing = subscriptionRepository.findByUserIdAndExhibitionId(userId, exhibitionId);
        if (existing.isEmpty() || existing.get().getStatus() == 0) {
            return "您当前未订阅该展览。";
        }
        ExhibitionSubscription sub = existing.get();
        sub.setStatus(0);
        subscriptionRepository.save(sub);
        return "已取消对《" + sub.getExhibition().getName() + "》的订阅。";
    }

    @Transactional(readOnly = true)
    public List<ExhibitionSubscription> listByUser(Long userId) {
        return subscriptionRepository.findByUserIdAndStatusOrderBySubscribedAtDesc(userId, 1);
    }

    /**
     * 订阅视图列表（事务内组装，避免 Jackson 序列化时懒加载异常）
     */
    @Transactional(readOnly = true)
    public List<com.pan.master_art.dto.SubscriptionView> listViewsByUser(Long userId) {
        List<ExhibitionSubscription> subs = subscriptionRepository
                .findByUserIdAndStatusOrderBySubscribedAtDesc(userId, 1);
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return subs.stream().map(s -> com.pan.master_art.dto.SubscriptionView.builder()
                .subscriptionId(s.getId())
                .exhibitionId(s.getExhibition().getId())
                .exhibitionName(s.getExhibition().getName())
                .exhibitionTheme(s.getExhibition().getTheme())
                .location(s.getExhibition().getLocation())
                .startTime(s.getExhibition().getStartTime() == null ? null
                        : fmt.format(s.getExhibition().getStartTime()))
                .status(s.getStatus())
                .subscribedAt(s.getSubscribedAt())
                .build()).collect(java.util.stream.Collectors.toList());
    }
}
