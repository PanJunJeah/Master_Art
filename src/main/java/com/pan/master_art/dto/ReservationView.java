package com.pan.master_art.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 真实预约视图（人工确认后产生）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationView {
    private Long reservationId;
    private Long draftId;
    private Long userId;
    private Long exhibitionId;
    private String exhibitionName;
    private Long sessionId;
    private String sessionTime;
    private Integer ticketCount;
    private BigDecimal totalPrice;
    private String status;
    private Date createTime;
}
