package com.pan.master_art.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 展览艺术品关联实体类
 * 用于管理展览和艺术品之间的多对多关系
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exhibition_artworks")
public class ExhibitionArtwork {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 展览ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;
    
    /**
     * 艺术品ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;
    
    /**
     * 在展览中的展示位置
     */
    @Column(length = 100)
    private String displayPosition;
    
    /**
     * 在展览中的重要性：1-普通，2-重点，3-核心
     */
    private Integer importance = 1;
    
    /**
     * 在展览中的描述（可能与艺术品自身描述不同）
     */
    @Column(columnDefinition = "TEXT")
    private String exhibitionDescription;
    
    /**
     * 在展览中的排序
     */
    private Integer sortOrder = 0;
    
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
