package com.pan.master_art.service;

import com.pan.master_art.entity.Artwork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 艺术品服务接口
 */
public interface ArtworkService {
    
    /**
     * 根据ID查询艺术品
     */
    Artwork findById(Long id);
    
    /**
     * 保存艺术品
     */
    Artwork save(Artwork artwork);
    
    /**
     * 更新艺术品
     */
    Artwork update(Artwork artwork);
    
    /**
     * 删除艺术品
     */
    void delete(Long id);
    
    /**
     * 分页查询所有艺术品
     */
    Page<Artwork> findAll(Pageable pageable);
    
    /**
     * 根据艺术品名称模糊查询
     */
    Page<Artwork> findByName(String name, Pageable pageable);
    
    /**
     * 根据艺术家ID查询艺术品
     */
    Page<Artwork> findByArtistId(Long artistId, Pageable pageable);
    
    /**
     * 根据艺术品类型查询
     */
    Page<Artwork> findByType(Integer type, Pageable pageable);
    
    /**
     * 根据艺术流派查询
     */
    Page<Artwork> findByGenre(String genre, Pageable pageable);
    
    /**
     * 根据创作年代查询
     */
    Page<Artwork> findByCreationYear(String creationYear, Pageable pageable);
    
    /**
     * 根据状态查询艺术品
     */
    Page<Artwork> findByStatus(Integer status, Pageable pageable);
    
    /**
     * 获取所有艺术品类型
     */
    List<Integer> findAllTypes();
    
    /**
     * 获取所有艺术流派
     */
    List<String> findAllGenres();
    
    /**
     * 获取所有创作年代
     */
    List<String> findAllCreationYears();
    
    /**
     * 获取所有材质
     */
    List<String> findAllMaterials();
    
    /**
     * 根据多个条件查询艺术品
     */
    Page<Artwork> findByConditions(String name, Long artistId, Integer type, String genre, String creationYear, Pageable pageable);
    
    /**
     * 查询浏览次数最多的艺术品
     */
    Page<Artwork> findMostViewed(Pageable pageable);
    
    /**
     * 根据收藏地点查询艺术品
     */
    Page<Artwork> findByCollectionPlace(String collectionPlace, Pageable pageable);
    
    /**
     * 更新艺术品状态
     */
    Artwork updateStatus(Long id, Integer status);
    
    /**
     * 增加艺术品浏览次数
     */
    void increaseViewCount(Long id);
    
    /**
     * 增加艺术品权重
     */
    void increaseWeight(Long id, Integer increment);
}