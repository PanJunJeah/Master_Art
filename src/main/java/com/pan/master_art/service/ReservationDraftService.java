package com.pan.master_art.service;

import com.pan.master_art.dto.DraftView;
import com.pan.master_art.entity.Exhibition;
import com.pan.master_art.entity.ReservationDraft;
import com.pan.master_art.entity.TicketSession;
import com.pan.master_art.exception.BizException;
import com.pan.master_art.repository.ExhibitionRepository;
import com.pan.master_art.repository.ReservationDraftRepository;
import com.pan.master_art.repository.TicketSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 预约草稿服务 —— Agent"人工确认闸门"的草稿侧
 *
 * 幂等设计（双层防护）：
 * 1. 应用层：先按 (userId, idempotencyKey) 查询，命中即重放，绝不重复创建；
 * 2. 数据库层：idempotency_key 唯一约束（uk_draft_idempotency），
 *    并发重试触发唯一键冲突时捕获 DataIntegrityViolationException 并降级为读取已有草稿。
 *
 * 注意：本服务不扣减任何库存；库存扣减只发生在用户人工确认后的 ReservationService.confirm。
 */
@Service
public class ReservationDraftService {

    private static final Logger log = LoggerFactory.getLogger(ReservationDraftService.class);

    private final ReservationDraftRepository draftRepository;
    private final TicketSessionRepository sessionRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final int ttlMinutes;

    public ReservationDraftService(ReservationDraftRepository draftRepository,
                                   TicketSessionRepository sessionRepository,
                                   ExhibitionRepository exhibitionRepository,
                                   @Value("${agent.draft-ttl-minutes:30}") int ttlMinutes) {
        this.draftRepository = draftRepository;
        this.sessionRepository = sessionRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.ttlMinutes = ttlMinutes;
    }

    /**
     * 幂等创建预约草稿（Agent 受限写工具 createReservationDraft 的唯一落点）
     *
     * @param idempotencyKey 幂等键。必须等于 canonicalKey(userId, exhibitionId, sessionId, ticketCount)；
     *                       传 "auto" 时服务端按规范生成并回传，模型重试时必须原样复用。
     */
    @Transactional
    public DraftCreationResult createDraft(Long userId, Long exhibitionId, Long sessionId,
                                           Integer ticketCount, String idempotencyKey) {
        if (userId == null || exhibitionId == null || sessionId == null) {
            throw new BizException("userId、exhibitionId、sessionId 均不能为空");
        }
        int count = ticketCount == null ? 1 : ticketCount;
        if (count < 1) {
            throw new BizException("预约票数至少为 1 张");
        }
        if (count > 6) {
            throw new BizException("单笔预约最多 6 张票，如需更多请分次预约");
        }

        // ---- 幂等键规范化：服务端推导规范键，防止模型自造键绕过去重 ----
        String canonical = canonicalKey(userId, exhibitionId, sessionId, count);
        String key;
        String trimmed = idempotencyKey == null ? "" : idempotencyKey.trim();
        if (trimmed.isEmpty() || "auto".equalsIgnoreCase(trimmed)) {
            key = canonical;
        } else if (!canonical.equals(trimmed)) {
            throw new BizException("幂等键不合法：应为 userId|exhibitionId|sessionId|ticketCount 的 SHA-256 哈希（" + canonical
                    + "），或传 auto 由服务端生成");
        } else {
            key = trimmed;
        }

        // ---- 第一层幂等：先查后建，命中即重放 ----
        Optional<ReservationDraft> existing = draftRepository.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            return new DraftCreationResult(toView(existing.get()), true);
        }

        // ---- 业务校验（不扣库存，仅软校验余票，硬校验在确认时再做） ----
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new BizException("展览不存在：id=" + exhibitionId));
        TicketSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("场次不存在：id=" + sessionId));
        if (!exhibition.getId().equals(session.getExhibition().getId())) {
            throw new BizException("场次 " + sessionId + " 不属于展览 " + exhibitionId);
        }
        if (session.getStatus() == null || session.getStatus() != 1) {
            throw new BizException("该场次已停止售票");
        }
        if (session.remainingTickets() < count) {
            throw new BizException("余票不足：该场次仅剩 " + session.remainingTickets() + " 张");
        }

        ReservationDraft draft = new ReservationDraft();
        draft.setUserId(userId);
        draft.setExhibition(exhibition);
        draft.setSession(session);
        draft.setSessionTime(session.getSessionTime());
        draft.setTicketCount(count);
        draft.setTotalPrice(session.getPrice().multiply(java.math.BigDecimal.valueOf(count)));
        draft.setIdempotencyKey(key);
        draft.setStatus(ReservationDraft.DraftStatus.PENDING);
        draft.setExpiresAt(new Date(System.currentTimeMillis() + ttlMinutes * 60_000L));

        try {
            draft = draftRepository.saveAndFlush(draft);
            log.info("预约草稿已创建 draftId={} userId={} idempotencyKey={}", draft.getId(), userId, key);
        } catch (DataIntegrityViolationException e) {
            // ---- 第二层幂等：唯一约束兜底，并发重试降级为读取已有草稿 ----
            log.warn("草稿唯一约束冲突，降级为幂等重放 userId={} key={}", userId, key);
            return draftRepository.findByUserIdAndIdempotencyKey(userId, key)
                    .map(d -> new DraftCreationResult(toView(d), true))
                    .orElseThrow(() -> new BizException("草稿创建冲突，请重试", e));
        }
        return new DraftCreationResult(toView(draft), false);
    }

    /**
     * 用户取消草稿（仅 PENDING 可取消；草稿不占库存，无需回补）
     */
    @Transactional
    public DraftView cancel(Long draftId, Long userId) {
        ReservationDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new BizException("草稿不存在：id=" + draftId));
        if (!draft.getUserId().equals(userId)) {
            throw new BizException("无权操作该草稿");
        }
        if (draft.getStatus() != ReservationDraft.DraftStatus.PENDING) {
            throw new BizException("当前草稿状态为 " + draft.getStatus() + "，无法取消");
        }
        draft.setStatus(ReservationDraft.DraftStatus.CANCELLED);
        return toView(draftRepository.save(draft));
    }

    /**
     * 读取草稿视图（事务内组装，避免懒加载问题）
     */
    @Transactional(readOnly = true)
    public DraftView getView(Long draftId) {
        return draftRepository.findById(draftId).map(this::toView).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ReservationDraft> listByUser(Long userId) {
        return draftRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 用户草稿视图列表（事务内组装，供「我的预约」页展示/续确认）
     */
    @Transactional(readOnly = true)
    public List<DraftView> listViewsByUser(Long userId) {
        return draftRepository.findByUserIdOrderByCreateTimeDesc(userId).stream()
                .map(this::toView)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 定时将过期且未确认的草稿置为 EXPIRED
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    public void expireOverdueDrafts() {
        List<ReservationDraft> overdue = draftRepository
                .findByStatusAndExpiresAtBefore(ReservationDraft.DraftStatus.PENDING, new Date());
        for (ReservationDraft draft : overdue) {
            draft.setStatus(ReservationDraft.DraftStatus.EXPIRED);
            draftRepository.save(draft);
        }
        if (!overdue.isEmpty()) {
            log.info("已过期草稿 {} 条", overdue.size());
        }
    }

    /**
     * 规范幂等键：SHA-256(userId|exhibitionId|sessionId|ticketCount)
     */
    public static String canonicalKey(Long userId, Long exhibitionId, Long sessionId, int ticketCount) {
        String raw = userId + "|" + exhibitionId + "|" + sessionId + "|" + ticketCount;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private DraftView toView(ReservationDraft draft) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return DraftView.builder()
                .draftId(draft.getId())
                .exhibitionId(draft.getExhibition().getId())
                .exhibitionName(draft.getExhibition().getName())
                .sessionId(draft.getSession().getId())
                .sessionTime(draft.getSessionTime() == null ? null : fmt.format(draft.getSessionTime()))
                .ticketCount(draft.getTicketCount())
                .totalPrice(draft.getTotalPrice())
                .status(draft.getStatus().name())
                .expiresAt(draft.getExpiresAt())
                .idempotencyKey(draft.getIdempotencyKey())
                .build();
    }
}
