package com.pan.master_art.service;

import com.pan.master_art.entity.Exhibition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

/**
 * 展览服务接口
 */
public interface ExhibitionService {
    
    /**
     * 根据ID查询展览
     */
    Exhibition findById(Long id);
    
    /**
     * 保存展览
     */
    Exhibition save(Exhibition exhibition);
    
    /**
     * 更新展览
     */
    Exhibition update(Exhibition exhibition);
    
    /**
     * 删除展览
     */
    void delete(Long id);
    
    /**
     * 分页查询所有展览
     */
    Page<Exhibition> findAll(Pageable pageable);
    
    /**
     * 根据展览名称模糊查询
     */
    Page<Exhibition> findByName(String name, Pageable pageable);
    
    /**
     * 根据展览类型查询
     */
    Page<Exhibition> findByType(Integer type, Pageable pageable);
    
    /**
     * 根据展览主题查询
     */
    Page<Exhibition> findByTheme(String theme, Pageable pageable);
    
    /**
     * 根据展览地点查询
     */
    Page<Exhibition> findByLocation(String location, Pageable pageable);
    
    /**
     * 查询当前正在进行的展览
     */
    Page<Exhibition> findCurrentExhibitions(Pageable pageable);
    
    /**
     * 查询即将开始的展览
     */
    Page<Exhibition> findUpcomingExhibitions(Pageable pageable);
    
    /**
     * 查询已结束的展览
     */
    Page<Exhibition> findPastExhibitions(Pageable pageable);
    
    /**
     * 根据策展人查询展览
     */
    Page<Exhibition> findByCurator(String curator, Pageable pageable);
    
    /**
     * 查询浏览次数最多的展览
     */
    Page<Exhibition> findMostViewed(Integer status, Pageable pageable);
    
    /**
     * 查询所有展览并按浏览次数降序排序
     */
    Page<Exhibition> findAllByOrderByViewCountDesc(Pageable pageable);
    
    /**
     * 根据多个条件查询展览
     */
    Page<Exhibition> findByConditions(String name, Integer type, String location, Date startTime, Date endTime, Integer status, Pageable pageable);
    
    /**
     * 获取所有展览地点
     */
    List<String> findAllLocations();
    
    /**
     * 获取所有展览主题
     */
    List<String> findAllThemes();
    
    /**
     * 更新展览状态
     */
    Exhibition updateStatus(Long id, Integer status);
    
    /**
     * 增加展览浏览次数
     */
    void increaseViewCount(Long id);
    
    /**
     * 增加展览权重
     */
    void increaseWeight(Long id, Integer increment);
    
    /**
     * 自动更新展览状态（定时任务）
     */
    void autoUpdateExhibitionStatus();

    /**
     * 查询浏览次数最多的展览（不考虑状态）
     */
    Page<Exhibition> findMostViewed(Pageable pageable);
    
}