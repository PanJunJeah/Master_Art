package com.pan.master_art.repository;

import com.pan.master_art.entity.Exhibition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 展览数据访问层
 */
@Repository
public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {
    
    /**
     * 根据展览名称模糊查询
     */
    Page<Exhibition> findByNameContainingOrEnglishNameContaining(String name, String englishName, Pageable pageable);
    
    /**
     * 根据展览类型查询
     */
    Page<Exhibition> findByType(Integer type, Pageable pageable);
    
    /**
     * 根据展览主题查询
     */
    Page<Exhibition> findByThemeContaining(String theme, Pageable pageable);
    
    /**
     * 根据展览地点查询
     */
    Page<Exhibition> findByLocationContaining(String location, Pageable pageable);
    
    /**
     * 查询当前正在进行的展览
     * 状态ONGOING表示进行中
     */
    @Query("SELECT e FROM Exhibition e WHERE e.startTime <= :now AND e.endTime >= :now AND e.status = :status ORDER BY e.weight DESC")
    Page<Exhibition> findCurrentExhibitions(@Param("now") Date now, @Param("status") Exhibition.ExhibitionStatus status, Pageable pageable);
    
    /**
     * 查询即将开始的展览
     */
    @Query("SELECT e FROM Exhibition e WHERE e.startTime > :now AND e.status = :status ORDER BY e.startTime ASC")
    Page<Exhibition> findUpcomingExhibitions(@Param("now") Date now, @Param("status") Exhibition.ExhibitionStatus status, Pageable pageable);
    
    /**
     * 查询已结束的展览
     */
    @Query("SELECT e FROM Exhibition e WHERE e.endTime < :now AND e.status = :status ORDER BY e.endTime DESC")
    Page<Exhibition> findPastExhibitions(@Param("now") Date now, @Param("status") Exhibition.ExhibitionStatus status, Pageable pageable);
    
    /**
     * 根据策展人查询展览
     */
    Page<Exhibition> findByCuratorContaining(String curator, Pageable pageable);
    
    /**
     * 查询浏览次数最多的展览
     */
    Page<Exhibition> findByStatusOrderByViewCountDesc(Exhibition.ExhibitionStatus status, Pageable pageable);

    /**
     * 统计某状态的展览数量（首页数据看板）
     */
    long countByStatus(Exhibition.ExhibitionStatus status);
    
    /**
     * 查询所有展览并按浏览次数降序排序
     */
    Page<Exhibition> findAllByOrderByViewCountDesc(Pageable pageable);
    
    /**
     * 根据多个条件查询展览
     * 注意：status 使用枚举参数（Hibernate 6 不支持枚举属性与整型字面量直接比较）
     */
    @Query("SELECT e FROM Exhibition e WHERE " +
           "(:name IS NULL OR e.name LIKE %:name% OR e.englishName LIKE %:name%) AND " +
           "(:type IS NULL OR e.type = :type) AND " +
           "(:location IS NULL OR e.location LIKE %:location%) AND " +
           "(:startTime IS NULL OR e.startTime >= :startTime) AND " +
           "(:endTime IS NULL OR e.endTime <= :endTime) AND " +
           "(:status IS NULL OR e.status = :status) " +
           "ORDER BY e.weight DESC")
    Page<Exhibition> findByConditions(
           @Param("name") String name, 
           @Param("type") Integer type, 
           @Param("location") String location, 
           @Param("startTime") Date startTime, 
           @Param("endTime") Date endTime, 
           @Param("status") Exhibition.ExhibitionStatus status,
           Pageable pageable);
    
    /**
     * 获取所有展览地点
     */
    @Query("SELECT DISTINCT e.location FROM Exhibition e WHERE e.location IS NOT NULL")
    List<String> findAllLocations();
    
    /**
     * 获取所有展览主题
     */
    @Query("SELECT DISTINCT e.theme FROM Exhibition e WHERE e.theme IS NOT NULL")
    List<String> findAllThemes();
}