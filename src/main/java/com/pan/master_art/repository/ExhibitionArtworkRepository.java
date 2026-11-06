package com.pan.master_art.repository;

import com.pan.master_art.entity.Artwork;
import com.pan.master_art.entity.Exhibition;
import com.pan.master_art.entity.ExhibitionArtwork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 展览艺术品关联数据访问层
 */
@Repository
public interface ExhibitionArtworkRepository extends JpaRepository<ExhibitionArtwork, Long> {
    
    /**
     * 根据展览ID查询关联的艺术品
     */
    List<ExhibitionArtwork> findByExhibitionIdOrderBySortOrderAsc(Long exhibitionId);
    
    /**
     * 根据艺术品ID查询关联的展览
     */
    List<ExhibitionArtwork> findByArtworkId(Long artworkId);
    
    /**
     * 根据展览ID和重要性查询关联的艺术品
     */
    List<ExhibitionArtwork> findByExhibitionIdAndImportanceOrderBySortOrderAsc(Long exhibitionId, Integer importance);
    
    /**
     * 根据展览ID分页查询关联的艺术品
     */
    Page<ExhibitionArtwork> findByExhibitionIdOrderBySortOrderAsc(Long exhibitionId, Pageable pageable);
    
    /**
     * 检查艺术品是否已经关联到展览
     */
    boolean existsByExhibitionIdAndArtworkId(Long exhibitionId, Long artworkId);
    
    /**
     * 根据展览ID删除所有关联
     */
    void deleteByExhibitionId(Long exhibitionId);
    
    /**
     * 根据艺术品ID删除所有关联
     */
    void deleteByArtworkId(Long artworkId);
    
    /**
     * 获取展览中的艺术品数量
     */
    @Query("SELECT COUNT(ea) FROM ExhibitionArtwork ea WHERE ea.exhibition.id = :exhibitionId")
    Long countByExhibitionId(@Param("exhibitionId") Long exhibitionId);
    
    /**
     * 获取艺术品参与的展览数量
     */
    @Query("SELECT COUNT(ea) FROM ExhibitionArtwork ea WHERE ea.artwork.id = :artworkId")
    Long countByArtworkId(@Param("artworkId") Long artworkId);
}