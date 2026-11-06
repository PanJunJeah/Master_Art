package com.pan.master_art.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 艺术品实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "artworks")
public class Artwork {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 艺术品名称
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * 艺术品英文名称
     */
    @Column(length = 100)
    private String englishName;
    
    /**
     * 创作年代
     */
    @Column(length = 50)
    private String creationYear;
    
    /**
     * 艺术家ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private Artist artist;
    
    /**
     * 艺术品类型：1-绘画，2-雕塑，3-装置，4-摄影，5-数字艺术，6-其他
     */
    private Integer type;
    
    /**
     * 艺术流派
     */
    @Column(length = 50)
    private String genre;
    
    /**
     * 材质
     */
    @Column(length = 100)
    private String material;
    
    /**
     * 尺寸（厘米）
     */
    @Column(length = 50)
    private String size;
    
    /**
     * 艺术品描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 艺术品故事
     */
    @Column(columnDefinition = "TEXT")
    private String story;
    
    /**
     * 艺术品主图URL
     */
    @Column(nullable = false)
    private String mainImageUrl;
    
    /**
     * 艺术品其他图片URL，以JSON数组形式存储
     */
    @Column(columnDefinition = "TEXT")
    private String imageUrls;
    
    /**
     * 3D模型URL（如果有）
     */
    private String modelUrl;
    
    /**
     * 收藏地点
     */
    @Column(length = 100)
    private String collectionPlace;
    
    /**
     * 历史意义
     */
    @Column(columnDefinition = "TEXT")
    private String historicalSignificance;
    
    /**
     * 艺术品特点
     */
    @Column(columnDefinition = "TEXT")
    private String features;
    
    /**
     * 浏览次数
     */
    private Integer viewCount = 0;
    
    /**
     * 排序权重
     */
    private Integer weight = 0;
    
    /**
     * 状态：0-下架，1-上架
     */
    @Column(nullable = false)
    private Integer status = 1;
    
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
