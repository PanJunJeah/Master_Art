package com.pan.master_art.repository;

import com.pan.master_art.entity.Artwork;
import com.pan.master_art.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 艺术品数据访问层
 */
@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    
    /**
     * 根据艺术品名称模糊查询
     */
    Page<Artwork> findByNameContainingOrEnglishNameContaining(String name, String englishName, Pageable pageable);
    
    /**
     * 根据艺术家查询艺术品
     */
    Page<Artwork> findByArtist(Artist artist, Pageable pageable);
    
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
     * 查询状态为上架的艺术品
     */
    Page<Artwork> findByStatus(Integer status, Pageable pageable);
    
    /**
     * 获取所有艺术品类型
     */
    @Query("SELECT DISTINCT a.type FROM Artwork a WHERE a.type IS NOT NULL")
    List<Integer> findAllTypes();
    
    /**
     * 获取所有艺术流派
     */
    @Query("SELECT DISTINCT a.genre FROM Artwork a WHERE a.genre IS NOT NULL")
    List<String> findAllGenres();
    
    /**
     * 获取所有创作年代
     */
    @Query("SELECT DISTINCT a.creationYear FROM Artwork a WHERE a.creationYear IS NOT NULL")
    List<String> findAllCreationYears();
    
    /**
     * 获取所有材质
     */
    @Query("SELECT DISTINCT a.material FROM Artwork a WHERE a.material IS NOT NULL")
    List<String> findAllMaterials();
    
    /**
     * 根据多个条件查询艺术品
     */
    @Query("SELECT a FROM Artwork a WHERE " +
           "(:name IS NULL OR a.name LIKE %:name% OR a.englishName LIKE %:name%) AND " +
           "(:artistId IS NULL OR a.artist.id = :artistId) AND " +
           "(:type IS NULL OR a.type = :type) AND " +
           "(:genre IS NULL OR a.genre = :genre) AND " +
           "(:creationYear IS NULL OR a.creationYear = :creationYear) AND " +
           "a.status = 1")
    Page<Artwork> findByConditions(
           @Param("name") String name, 
           @Param("artistId") Long artistId, 
           @Param("type") Integer type, 
           @Param("genre") String genre, 
           @Param("creationYear") String creationYear, 
           Pageable pageable);
    
    /**
     * 查询浏览次数最多的艺术品
     */
    Page<Artwork> findByStatusOrderByViewCountDesc(Integer status, Pageable pageable);
    
    /**
     * 根据收藏地点查询艺术品
     */
    Page<Artwork> findByCollectionPlace(String collectionPlace, Pageable pageable);

    /**
     * 统计某状态的艺术品数量（首页数据看板）
     */
    long countByStatus(Integer status);

    /**
     * Agent 工具 searchArtworks 专用检索：fetch join 艺术家，避免懒加载问题
     */
    @Query("SELECT a FROM Artwork a LEFT JOIN FETCH a.artist WHERE a.status = 1 AND " +
           "(:name IS NULL OR a.name LIKE %:name% OR a.englishName LIKE %:name%) AND " +
           "(:artistName IS NULL OR a.artist.name LIKE %:artistName%) AND " +
           "(:type IS NULL OR a.type = :type) AND " +
           "(:genre IS NULL OR a.genre = :genre) AND " +
           "(:creationYear IS NULL OR a.creationYear = :creationYear)")
    Page<Artwork> searchForAgent(
           @Param("name") String name,
           @Param("artistName") String artistName,
           @Param("type") Integer type,
           @Param("genre") String genre,
           @Param("creationYear") String creationYear,
           Pageable pageable);

    /**
     * RAG 知识检索召回：对名称/描述/故事/流派/艺术家名做关键词召回
     */
    @Query("SELECT a FROM Artwork a LEFT JOIN FETCH a.artist WHERE a.status = 1 AND " +
           "(a.name LIKE %:kw% OR a.englishName LIKE %:kw% OR a.description LIKE %:kw% " +
           "OR a.story LIKE %:kw% OR a.genre LIKE %:kw% OR a.artist.name LIKE %:kw%)")
    List<Artwork> findRagCandidates(@Param("kw") String kw);
}