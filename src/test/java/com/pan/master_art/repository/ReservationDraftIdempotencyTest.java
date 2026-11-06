package com.pan.master_art.repository;

import com.pan.master_art.entity.Exhibition;
import com.pan.master_art.entity.ReservationDraft;
import com.pan.master_art.entity.TicketSession;
import com.pan.master_art.exception.BizException;
import com.pan.master_art.service.DraftCreationResult;
import com.pan.master_art.service.ReservationDraftService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幂等控制的数据库层验证（H2 内存库，无需本地 MySQL）：
 * - idempotency_key 唯一约束（uk_draft_idempotency）真实存在；
 * - 服务层"先查后建 + 唯一约束兜底"在真实 JPA 栈上生效。
 */
@DataJpaTest
class ReservationDraftIdempotencyTest {

    @Autowired
    private ReservationDraftRepository draftRepository;

    @Autowired
    private TicketSessionRepository sessionRepository;

    @Autowired
    private ExhibitionRepository exhibitionRepository;

    private ReservationDraftService newService() {
        return new ReservationDraftService(draftRepository, sessionRepository, exhibitionRepository, 30);
    }

    private Exhibition saveExhibition() {
        Exhibition e = new Exhibition();
        e.setName("印象派大师展");
        e.setTheme("印象派");
        e.setLocation("上海");
        e.setStartTime(new Date(System.currentTimeMillis() - 86400_000L));
        e.setEndTime(new Date(System.currentTimeMillis() + 7 * 86400_000L));
        e.setStatus(Exhibition.ExhibitionStatus.ONGOING);
        return exhibitionRepository.save(e);
    }

    private TicketSession saveSession(Exhibition exhibition) {
        TicketSession s = new TicketSession();
        s.setExhibition(exhibition);
        s.setSessionTime(new Date(System.currentTimeMillis() + 2 * 86400_000L));
        s.setTotalTickets(100);
        s.setSoldTickets(30);
        s.setPrice(new BigDecimal("88.00"));
        s.setStatus(1);
        return sessionRepository.save(s);
    }

    private ReservationDraft newDraft(Long userId, Exhibition ex, TicketSession session, String key) {
        ReservationDraft d = new ReservationDraft();
        d.setUserId(userId);
        d.setExhibition(ex);
        d.setSession(session);
        d.setSessionTime(session.getSessionTime());
        d.setTicketCount(1);
        d.setTotalPrice(session.getPrice());
        d.setIdempotencyKey(key);
        d.setStatus(ReservationDraft.DraftStatus.PENDING);
        d.setExpiresAt(new Date(System.currentTimeMillis() + 30 * 60_000L));
        return d;
    }

    @Test
    @DisplayName("同一幂等键重复创建 → 服务层重放，数据库仅一条草稿")
    void duplicateIntentReplaysInsteadOfDuplicating() {
        Exhibition ex = saveExhibition();
        TicketSession session = saveSession(ex);
        ReservationDraftService service = newService();

        DraftCreationResult first = service.createDraft(42L, ex.getId(), session.getId(), 2, "auto");
        DraftCreationResult second = service.createDraft(42L, ex.getId(), session.getId(), 2,
                first.getView().getIdempotencyKey());

        assertFalse(first.isReplayed());
        assertTrue(second.isReplayed(), "第二次调用必须幂等重放");
        assertEquals(first.getView().getDraftId(), second.getView().getDraftId());
        assertEquals(1, draftRepository.count(), "数据库中只能存在一条草稿");
    }

    @Test
    @DisplayName("数据库唯一约束 uk_draft_idempotency 兜底：绕过服务层直插重复键被拒绝")
    void databaseUniqueConstraintIsTheLastLineOfDefense() {
        Exhibition ex = saveExhibition();
        TicketSession session = saveSession(ex);
        String key = ReservationDraftService.canonicalKey(42L, ex.getId(), session.getId(), 1);

        draftRepository.saveAndFlush(newDraft(42L, ex, session, key));
        assertThrows(DataIntegrityViolationException.class,
                () -> draftRepository.saveAndFlush(newDraft(42L, ex, session, key)),
                "唯一约束必须拒绝重复幂等键");
    }

    @Test
    @DisplayName("非法幂等键被服务层拒绝")
    void invalidIdempotencyKeyRejected() {
        Exhibition ex = saveExhibition();
        TicketSession session = saveSession(ex);

        BizException exception = assertThrows(BizException.class,
                () -> newService().createDraft(42L, ex.getId(), session.getId(), 1, "fake-key"));
        assertTrue(exception.getMessage().contains("幂等键不合法"));
    }

    @Test
    @DisplayName("场次不属于该展览时拒绝生成草稿")
    void sessionOfAnotherExhibitionRejected() {
        Exhibition ex1 = saveExhibition();
        Exhibition ex2 = saveExhibition();
        TicketSession sessionOfEx1 = saveSession(ex1);

        BizException exception = assertThrows(BizException.class,
                () -> newService().createDraft(42L, ex2.getId(), sessionOfEx1.getId(), 1, "auto"));
        assertTrue(exception.getMessage().contains("不属于"));
    }
}
