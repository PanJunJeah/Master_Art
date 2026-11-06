package com.pan.master_art.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 展览实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exhibitions")
public class Exhibition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 展览名称
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * 展览英文名称
     */
    @Column(length = 100)
    private String englishName;
    
    /**
     * 展览主题
     */
    @Column(length = 100)
    private String theme;
    
    /**
     * 展览描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 展览地点
     */
    @Column(length = 100)
    private String location;
    
    /**
     * 开始时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date startTime;
    
    /**
     * 结束时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date endTime;
    
    /**
     * 展览海报URL
     */
    private String posterUrl;
    
    /**
     * 展览图片URL，以JSON数组形式存储
     */
    @Column(name = "cover_image_url")
    private String coverImageUrl;
    /**
     * 展览类型：1-常规展览，2-特别展览，3-临时展览，4-巡回展览
     */
    private Integer type = 1;
    
    /**
     * 展览策展人
     */
    @Column(length = 100)
    private String curator;
    
    /**
     * 展览亮点
     */
    @Column(columnDefinition = "TEXT")
    private String highlights;
    
    /**
     * 参观信息（开放时间、票价等）
     */
    @Column(columnDefinition = "TEXT")
    private String visitInfo;
    
    /**
     * 浏览次数
     */
    private Integer viewCount = 0;
    
    /**
     * 排序权重
     */
    private Integer weight = 0;
    
    /**
     * 状态：0-未开始，1-进行中，2-已结束
     * 数据库中定义为ENUM('UPCOMING', 'ONGOING', 'ENDED')
     * 对应关系：0-UPCOMING, 1-ONGOING, 2-ENDED
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExhibitionStatus status = ExhibitionStatus.UPCOMING;
    
    /**
     * 展览状态枚举
     */
    public enum ExhibitionStatus {
        UPCOMING, // 未开始
        ONGOING,  // 进行中
        ENDED     // 已结束
    }
    
    /**
     * 创建时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createTime;
    
    /**
     * 更新时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
