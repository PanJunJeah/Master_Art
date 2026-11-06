package com.pan.master_art.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 展览排期（场次）视图：排期 + 余票
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionView {
    private Long sessionId;
    private Long exhibitionId;
    private String exhibitionName;
    private String sessionTime;
    private BigDecimal price;
    private Integer totalTickets;
    private Integer soldTickets;
    /** 余票 = totalTickets - soldTickets */
    private Integer remaining;
    /** 1-售票中 0-停售 */
    private Integer status;
}
