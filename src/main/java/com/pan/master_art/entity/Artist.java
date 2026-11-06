package com.pan.master_art.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 艺术家实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "artists")
public class Artist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 艺术家姓名
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * 艺术家英文名
     */
    @Column(length = 100)
    private String englishName;
    
    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender = 0;
    
    /**
     * 出生日期
     */
    @Temporal(TemporalType.DATE)
    private Date birthDate;
    
    /**
     * 逝世日期（如果适用）
     */
    @Temporal(TemporalType.DATE)
    private Date deathDate;
    
    /**
     * 国籍
     */
    @Column(length = 50)
    private String nationality;
    
    /**
     * 艺术流派
     */
    @Column(length = 50)
    private String genre;
    
    /**
     * 艺术家简介
     */
    @Column(columnDefinition = "TEXT")
    private String biography;
    
    /**
     * 艺术家照片URL
     */
    private String photoUrl;
    
    /**
     * 代表作品
     */
    @Column(length = 255)
    private String representativeWorks;
    
    /**
     * 历史地位和影响
     */
    @Column(columnDefinition = "TEXT")
    private String historicalSignificance;
    
    /**
     * 创作时期
     */
    @Column(length = 100)
    private String creationPeriod;
    
    /**
     * 排序权重
     */
    private Integer weight = 0;
    
    /**
     * 状态：0-禁用，1-启用
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
