package com.pan.master_art.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 新展订阅视图（在事务内组装，避免懒加载序列化问题）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionView {
    private Long subscriptionId;
    private Long exhibitionId;
    private String exhibitionName;
    private String exhibitionTheme;
    private String location;
    /** 开展时间（格式化字符串） */
    private String startTime;
    /** 1-订阅中 0-已取消 */
    private Integer status;
    private Date subscribedAt;
}
