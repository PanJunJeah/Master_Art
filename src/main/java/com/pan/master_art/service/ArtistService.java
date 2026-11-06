package com.pan.master_art.service;

import com.pan.master_art.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 艺术家服务接口
 */
public interface ArtistService {
    
    /**
     * 根据ID查询艺术家
     */
    Artist findById(Long id);
    
    /**
     * 保存艺术家
     */
    Artist save(Artist artist);
    
    /**
     * 更新艺术家
     */
    Artist update(Artist artist);
    
    /**
     * 删除艺术家
     */
    void delete(Long id);
    
    /**
     * 分页查询所有艺术家
     */
    Page<Artist> findAll(Pageable pageable);
    
    /**
     * 根据艺术家姓名模糊查询
     */
    Page<Artist> findByName(String name, Pageable pageable);
    
    /**
     * 根据国籍查询艺术家
     */
    Page<Artist> findByNationality(String nationality, Pageable pageable);
    
    /**
     * 根据艺术流派查询艺术家
     */
    Page<Artist> findByGenre(String genre, Pageable pageable);
    
    /**
     * 根据创作时期查询艺术家
     */
    Page<Artist> findByCreationPeriod(String creationPeriod, Pageable pageable);
    
    /**
     * 根据状态查询艺术家
     */
    Page<Artist> findByStatus(Integer status, Pageable pageable);
    
    /**
     * 获取所有艺术流派
     */
    List<String> findAllGenres();
    
    /**
     * 获取所有国籍
     */
    List<String> findAllNationalities();
    
    /**
     * 获取所有创作时期
     */
    List<String> findAllCreationPeriods();
    
    /**
     * 根据多个条件查询艺术家
     */
    Page<Artist> findByConditions(String name, String nationality, String genre, String creationPeriod, Pageable pageable);
    
    /**
     * 更新艺术家状态
     */
    Artist updateStatus(Long id, Integer status);
    
    /**
     * 增加艺术家权重
     */
    void increaseWeight(Long id, Integer increment);

    void increaseViewCount(Long id);

    Page<Artist> findByPeriod(String period, Pageable pageable);

    Page<Artist> findMostViewed(Pageable pageable);
}