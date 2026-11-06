package com.pan.master_art.repository;

import com.pan.master_art.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 艺术家数据访问层
 */
@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    
    /**
     * 根据艺术家姓名模糊查询
     */
    Page<Artist> findByNameContainingOrEnglishNameContaining(String name, String englishName, Pageable pageable);
    
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
     * 查询状态为启用的艺术家
     */
    Page<Artist> findByStatus(Integer status, Pageable pageable);

    /**
     * 统计某状态的艺术家数量（首页数据看板）
     */
    long countByStatus(Integer status);
    
    /**
     * 获取所有艺术流派
     */
    @Query("SELECT DISTINCT a.genre FROM Artist a WHERE a.genre IS NOT NULL")
    List<String> findAllGenres();
    
    /**
     * 获取所有国籍
     */
    @Query("SELECT DISTINCT a.nationality FROM Artist a WHERE a.nationality IS NOT NULL")
    List<String> findAllNationalities();
    
    /**
     * 获取所有创作时期
     */
    @Query("SELECT DISTINCT a.creationPeriod FROM Artist a WHERE a.creationPeriod IS NOT NULL")
    List<String> findAllCreationPeriods();
    
    /**
     * 根据多个条件查询艺术家
     */
    @Query("SELECT a FROM Artist a WHERE " +
           "(:name IS NULL OR a.name LIKE %:name% OR a.englishName LIKE %:name%) AND " +
           "(:nationality IS NULL OR a.nationality = :nationality) AND " +
           "(:genre IS NULL OR a.genre = :genre) AND " +
           "(:creationPeriod IS NULL OR a.creationPeriod = :creationPeriod) AND " +
           "a.status = 1 ORDER BY a.weight DESC")
    Page<Artist> findByConditions(
           @Param("name") String name, 
           @Param("nationality") String nationality, 
           @Param("genre") String genre, 
           @Param("creationPeriod") String creationPeriod, 
           Pageable pageable);
}