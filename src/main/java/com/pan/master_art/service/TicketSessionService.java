package com.pan.master_art.service;

import com.pan.master_art.dto.SessionView;
import com.pan.master_art.entity.TicketSession;
import com.pan.master_art.repository.TicketSessionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 展览排期（场次）查询服务
 * 供 Agent 只读工具 queryExhibition 与前端票务区共用，只读，不触碰库存。
 */
@Service
public class TicketSessionService {

    private final TicketSessionRepository sessionRepository;

    public TicketSessionService(TicketSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public List<SessionView> sessionsOf(Long exhibitionId, boolean onSaleOnly) {
        Sort sort = Sort.by(Sort.Direction.ASC, "sessionTime");
        List<TicketSession> sessions = onSaleOnly
                ? sessionRepository.findByExhibitionIdAndStatus(exhibitionId, 1, sort)
                : sessionRepository.findByExhibitionId(exhibitionId, sort);
        return sessions.stream().map(this::toView).collect(Collectors.toList());
    }

    private SessionView toView(TicketSession s) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return SessionView.builder()
                .sessionId(s.getId())
                .exhibitionId(s.getExhibition().getId())
                .exhibitionName(s.getExhibition().getName())
                .sessionTime(s.getSessionTime() == null ? null : fmt.format(s.getSessionTime()))
                .price(s.getPrice())
                .totalTickets(s.getTotalTickets())
                .soldTickets(s.getSoldTickets())
                .remaining(s.remainingTickets())
                .status(s.getStatus())
                .build();
    }
}
