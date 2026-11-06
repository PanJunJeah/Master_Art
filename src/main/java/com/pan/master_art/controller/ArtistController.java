package com.pan.master_art.controller;

import com.pan.master_art.entity.Artist;
import com.pan.master_art.entity.Artwork;
import com.pan.master_art.service.ArtistService;
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
 * 艺术家控制器
 */
@RestController
@RequestMapping("/api/artists")
//@CrossOrigin(origins = "*", maxAge = 3600)
public class ArtistController {

    @Autowired
    private ArtistService artistService;
    
    @Autowired
    private ArtworkService artworkService;
    
    /**
     * 获取艺术家详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Artist> getArtist(@PathVariable Long id) {
        Artist artist = artistService.findById(id);
        if (artist != null) {
            // 增加浏览次数
            artistService.increaseViewCount(id);
            return ResponseEntity.ok(artist);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 创建艺术家
     */
    @PostMapping
    public ResponseEntity<Artist> createArtist(@RequestBody Artist artist) {
        return ResponseEntity.ok(artistService.save(artist));
    }
    
    /**
     * 更新艺术家
     */
    @PutMapping("/{id}")
    public ResponseEntity<Artist> updateArtist(@PathVariable Long id, @RequestBody Artist artist) {
        artist.setId(id);
        Artist updatedArtist = artistService.update(artist);
        if (updatedArtist != null) {
            return ResponseEntity.ok(updatedArtist);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除艺术家
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtist(@PathVariable Long id) {
        artistService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 分页查询艺术家列表
     */
    @GetMapping("/getAllArtists")
    public ResponseEntity<Map<String, Object>> getAllArtists(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
    
        if (limit != null && limit > 0) {
            size = limit;
        }
    
        Pageable pageable;
        Page<Artist> artistPage;
    
        if ("popularity".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "weight"));
            artistPage = artistService.findMostViewed(pageable);
        } else {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            artistPage = artistService.findAll(pageable);
        }
    
        Map<String, Object> response = new HashMap<>();
        response.put("content", artistPage.getContent());
        response.put("currentPage", artistPage.getNumber());
        response.put("totalItems", artistPage.getTotalElements());
        response.put("totalPages", artistPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取艺术家的作品列表
     */
    @GetMapping("/{id}/artworks")
    public ResponseEntity<Map<String, Object>> getArtistArtworks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Artist artist = artistService.findById(id);
        if (artist == null) {
            return ResponseEntity.notFound().build();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Artwork> artworkPage = artworkService.findByArtistId(id, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", artworkPage.getContent());
        response.put("currentPage", artworkPage.getNumber());
        response.put("totalItems", artworkPage.getTotalElements());
        response.put("totalPages", artworkPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取最受欢迎的艺术家
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularArtists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "weight"));
        Page<Artist> artistPage = artistService.findMostViewed(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", artistPage.getContent());
        response.put("currentPage", artistPage.getNumber());
        response.put("totalItems", artistPage.getTotalElements());
        response.put("totalPages", artistPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新艺术家状态
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Artist> updateArtistStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        
        Artist artist = artistService.updateStatus(id, status);
        if (artist != null) {
            return ResponseEntity.ok(artist);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取所有国籍
     */
    @GetMapping("/nationalities")
    public ResponseEntity<List<String>> getAllNationalities() {
        return ResponseEntity.ok(artistService.findAllNationalities());
    }
    
    /**
     * 获取所有艺术流派
     */
    @GetMapping("/genres")
    public ResponseEntity<List<String>> getAllGenres() {
        return ResponseEntity.ok(artistService.findAllGenres());
    }
    
    /**
     * 根据时期查询艺术家
     */
    @GetMapping("/by-period")
    public ResponseEntity<Map<String, Object>> getArtistsByPeriod(
            @RequestParam String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Artist> artistPage = artistService.findByPeriod(period, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", artistPage.getContent());
        response.put("currentPage", artistPage.getNumber());
        response.put("totalItems", artistPage.getTotalElements());
        response.put("totalPages", artistPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
}