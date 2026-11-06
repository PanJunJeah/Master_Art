package com.pan.master_art.service;

import com.pan.master_art.dto.ReservationView;
import com.pan.master_art.entity.Reservation;
import com.pan.master_art.entity.ReservationDraft;
import com.pan.master_art.exception.BizException;
import com.pan.master_art.repository.ReservationDraftRepository;
import com.pan.master_art.repository.ReservationRepository;
import com.pan.master_art.repository.TicketSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 真实预约服务 —— 人工确认闸门的"真实写入侧"
 *
 * 安全边界（面试重点）：
 * 1. 本服务的方法【永不】被注册为 Agent 工具（@Tool），大模型无任何路径调用 confirm()；
 * 2. 只有用户在前端看到预约草稿卡片、人工点击"确认预约"后，
 *    Vue3 才会直接调用 POST /api/reservations/confirm 触发本服务；
 * 3. 库存扣减通过 TicketSessionRepository.tryOccupyTickets 的"条件 UPDATE"原子完成，
 *    余票不足时受影响行数为 0，直接拒绝，天然防超卖；
 * 4. 草稿→真实预约一对一（draft_id 唯一约束 + 状态守卫），确认接口本身幂等。
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final ReservationDraftRepository draftRepository;
    private final TicketSessionRepository sessionRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationDraftRepository draftRepository,
                              TicketSessionRepository sessionRepository) {
        this.reservationRepository = reservationRepository;
        this.draftRepository = draftRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * 人工确认预约草稿，生成真实预约并原子扣减库存。
     * 仅由前端直接调用，Agent 永远无法触达。
     */
    @Transactional
    public ReservationView confirm(Long draftId, Long userId) {
        if (draftId == null || userId == null) {
            throw new BizException("draftId 与 userId 不能为空");
        }
        ReservationDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new BizException("预约草稿不存在：id=" + draftId));

        // 归属校验：只能确认自己的草稿
        if (!draft.getUserId().equals(userId)) {
            throw new BizException("无权确认该预约草稿");
        }

        // 状态守卫（幂等）：已确认的草稿直接返回已有预约
        if (draft.getStatus() == ReservationDraft.DraftStatus.CONFIRMED) {
            return reservationRepository.findByDraftId(draftId)
                    .map(this::toView)
                    .orElseThrow(() -> new BizException("草稿状态异常：标记为已确认但未找到对应预约"));
        }
        if (draft.getStatus() != ReservationDraft.DraftStatus.PENDING) {
            throw new BizException("草稿状态为 " + draft.getStatus() + "，无法确认。"
                    + (draft.getStatus() == ReservationDraft.DraftStatus.EXPIRED
                        ? "草稿已过期，请让 AI 助手重新生成预约草稿。" : ""));
        }
        if (draft.getExpiresAt() != null && draft.getExpiresAt().before(new Date())) {
            draft.setStatus(ReservationDraft.DraftStatus.EXPIRED);
            draftRepository.save(draft);
            throw new BizException("草稿已过期（有效期至 "
                    + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(draft.getExpiresAt())
                    + "），请让 AI 助手重新生成预约草稿");
        }

        // 原子扣减库存（条件 UPDATE），防超卖
        int affected = sessionRepository.tryOccupyTickets(draft.getSession().getId(), draft.getTicketCount());
        if (affected == 0) {
            throw new BizException("余票不足或场次已停售，预约失败，请重新选择场次");
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setExhibition(draft.getExhibition());
        reservation.setSession(draft.getSession());
        reservation.setDraft(draft);
        reservation.setTicketCount(draft.getTicketCount());
        reservation.setTotalPrice(draft.getTotalPrice());
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        reservation = reservationRepository.save(reservation);

        draft.setStatus(ReservationDraft.DraftStatus.CONFIRMED);
        draftRepository.save(draft);

        log.info("预约确认成功 reservationId={} draftId={} userId={}", reservation.getId(), draftId, userId);
        return toView(reservation);
    }

    /**
     * 取消已确认的预约并回补库存（同样不向 Agent 开放）
     */
    @Transactional
    public ReservationView cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BizException("预约不存在：id=" + reservationId));
        if (!reservation.getUserId().equals(userId)) {
            throw new BizException("无权取消该预约");
        }
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new BizException("预约状态为 " + reservation.getStatus() + "，无法取消");
        }
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        sessionRepository.releaseTickets(reservation.getSession().getId(), reservation.getTicketCount());
        return toView(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public List<ReservationView> listByUser(Long userId) {
        return reservationRepository.findByUserIdOrderByCreateTimeDesc(userId).stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    private ReservationView toView(Reservation r) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return ReservationView.builder()
                .reservationId(r.getId())
                .draftId(r.getDraft() == null ? null : r.getDraft().getId())
                .userId(r.getUserId())
                .exhibitionId(r.getExhibition().getId())
                .exhibitionName(r.getExhibition().getName())
                .sessionId(r.getSession().getId())
                .sessionTime(r.getSession().getSessionTime() == null ? null : fmt.format(r.getSession().getSessionTime()))
                .ticketCount(r.getTicketCount())
                .totalPrice(r.getTotalPrice())
                .status(r.getStatus().name())
                .createTime(r.getCreateTime())
                .build();
    }
}
