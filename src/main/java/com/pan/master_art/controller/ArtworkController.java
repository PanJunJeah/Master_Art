package com.pan.master_art.controller;

import com.pan.master_art.entity.Artwork;
import com.pan.master_art.service.ArtworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 艺术品控制器
 */
@RestController
@RequestMapping("/api/artworks")
//@CrossOrigin(origins = "*", maxAge = 3600)
public class ArtworkController {

    @Autowired
    private ArtworkService artworkService;
    
    /**
     * 获取艺术品详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Artwork> getArtwork(@PathVariable Long id) {
        Artwork artwork = artworkService.findById(id);
        if (artwork != null) {
            // 增加浏览次数
            artworkService.increaseViewCount(id);
            return ResponseEntity.ok(artwork);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 创建艺术品
     */
    @PostMapping
    public ResponseEntity<Artwork> createArtwork(@RequestBody Artwork artwork) {
        return ResponseEntity.ok(artworkService.save(artwork));
    }
    
    /**
     * 更新艺术品
     */
    @PutMapping("/{id}")
    public ResponseEntity<Artwork> updateArtwork(@PathVariable Long id, @RequestBody Artwork artwork) {
        artwork.setId(id);
        Artwork updatedArtwork = artworkService.update(artwork);
        if (updatedArtwork != null) {
            return ResponseEntity.ok(updatedArtwork);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除艺术品
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtwork(@PathVariable Long id) {
        artworkService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 分页查询艺术品列表
     */
    @GetMapping("/getAllArtworks")
    public ResponseEntity<Map<String, Object>> getArtworks(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long artistId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String creationYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = Sort.by(direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Artwork> artworkPage;
        if (name != null || artistId != null || type != null || genre != null || creationYear != null) {
            artworkPage = artworkService.findByConditions(name, artistId, type, genre, creationYear, pageable);
        } else {
            artworkPage = artworkService.findAll(pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", artworkPage.getContent());
        response.put("currentPage", artworkPage.getNumber());
        response.put("totalItems", artworkPage.getTotalElements());
        response.put("totalPages", artworkPage.getTotalPages());
        
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
    
    /**
     * 获取艺术品列表（根路径请求）
     * 支持按受欢迎程度排序和限制数量
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllArtworks(
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) Integer limit,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    
        if (limit != null && limit > 0) {
            size = limit;
        }
    
        Pageable pageable;
        Page<Artwork> artworkPage;
    
        if ("popularity".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "viewCount"));
            artworkPage = artworkService.findMostViewed(pageable);
        } else {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            artworkPage = artworkService.findAll(pageable);
        }
    
        Map<String, Object> response = new HashMap<>();
        response.put("content", artworkPage.getContent());
        response.put("currentPage", artworkPage.getNumber());
        response.put("totalItems", artworkPage.getTotalElements());
        response.put("totalPages", artworkPage.getTotalPages());
        
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
    
    /**
     * 获取最受欢迎的艺术品
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularArtworks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Artwork> artworkPage = artworkService.findMostViewed(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", artworkPage.getContent());
        response.put("currentPage", artworkPage.getNumber());
        response.put("totalItems", artworkPage.getTotalElements());
        response.put("totalPages", artworkPage.getTotalPages());
        
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
    
    /**
     * 更新艺术品状态
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Artwork> updateArtworkStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        
        Artwork artwork = artworkService.updateStatus(id, status);
        if (artwork != null) {
            return ResponseEntity.ok(artwork);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取所有艺术品类型
     */
    @GetMapping("/types")
    public ResponseEntity<List<Integer>> getAllTypes() {
        return ResponseEntity.ok(artworkService.findAllTypes());
    }
    
    /**
     * 获取所有艺术流派
     */
    @GetMapping("/genres")
    public ResponseEntity<List<String>> getAllGenres() {
        return ResponseEntity.ok(artworkService.findAllGenres());
    }
    
    /**
     * 获取所有创作年代
     */
    @GetMapping("/creation-years")
    public ResponseEntity<List<String>> getAllCreationYears() {
        return ResponseEntity.ok(artworkService.findAllCreationYears());
    }
    
    /**
     * 获取所有材质
     */
    @GetMapping("/materials")
    public ResponseEntity<List<String>> getAllMaterials() {
        return ResponseEntity.ok(artworkService.findAllMaterials());
    }
    
    /**
     * 根据收藏地点查询艺术品
     */
    @GetMapping("/by-collection-place")
    public ResponseEntity<Map<String, Object>> getArtworksByCollectionPlace(
            @RequestParam String collectionPlace,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Artwork> artworkPage = artworkService.findByCollectionPlace(collectionPlace, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", artworkPage.getContent());
        response.put("currentPage", artworkPage.getNumber());
        response.put("totalItems", artworkPage.getTotalElements());
        response.put("totalPages", artworkPage.getTotalPages());
        
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
}