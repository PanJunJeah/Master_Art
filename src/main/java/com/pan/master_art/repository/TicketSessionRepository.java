package com.pan.master_art.repository;

import com.pan.master_art.entity.TicketSession;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 展览排期（场次）数据访问层
 */
@Repository
public interface TicketSessionRepository extends JpaRepository<TicketSession, Long> {

    /**
     * 查询某展览的全部场次（可按状态过滤）
     */
    List<TicketSession> findByExhibitionId(Long exhibitionId, Sort sort);

    List<TicketSession> findByExhibitionIdAndStatus(Long exhibitionId, Integer status, Sort sort);

    /**
     * 条件占用票数（原子扣减）：
     * 只有当 总票数 - 已售票数 >= n 且场次在售时，才执行 +n。
     * 返回受影响行数：1-扣减成功，0-余票不足。
     * 这是库存扣减的唯一入口，仅在人工确认预约草稿后由 ReservationService.confirm 调用，
     * Agent 工具层无任何路径触达此处。
     */
    @Modifying
    @Query("UPDATE TicketSession t SET t.soldTickets = t.soldTickets + :n " +
           "WHERE t.id = :sessionId AND t.status = 1 AND t.totalTickets - t.soldTickets >= :n")
    int tryOccupyTickets(@Param("sessionId") Long sessionId, @Param("n") int n);

    /**
     * 释放票数（取消预约时回补，同样不向 Agent 开放）
     */
    @Modifying
    @Query("UPDATE TicketSession t SET t.soldTickets = t.soldTickets - :n " +
           "WHERE t.id = :sessionId AND t.soldTickets >= :n")
    int releaseTickets(@Param("sessionId") Long sessionId, @Param("n") int n);
}
