package com.pan.master_art.controller;

import com.pan.master_art.entity.Exhibition;
import com.pan.master_art.service.ExhibitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 展览控制器
 */
@RestController
@RequestMapping("/api/exhibitions")
//@CrossOrigin(origins = "*", maxAge = 3600)
public class ExhibitionController {

    @Autowired
    private ExhibitionService exhibitionService;
    
    /**
     * 获取展览列表（用于前端调用）
     */
    @GetMapping("/get-list")
    public ResponseEntity<Map<String, Object>> getExhibitionList(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        // 如果指定了limit参数，则使用limit作为size
        if (limit != null && limit > 0) {
            size = limit;
        }
        
        Pageable pageable;
        Page<Exhibition> exhibitionPage;
        
        // 如果sort=popularity，则按浏览次数排序
        if ("popularity".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "viewCount"));
            // 不传入status参数，获取所有展览并按浏览次数排序
            exhibitionPage = exhibitionService.findAllByOrderByViewCountDesc(pageable);
        } else {
            // 默认按开始时间倒序排序
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
            exhibitionPage = exhibitionService.findAll(pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", exhibitionPage.getContent());
        response.put("currentPage", exhibitionPage.getNumber());
        response.put("totalItems", exhibitionPage.getTotalElements());
        response.put("totalPages", exhibitionPage.getTotalPages());
        
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
    
    /**
     * 获取展览详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Exhibition> getExhibition(@PathVariable Long id) {
        Exhibition exhibition = exhibitionService.findById(id);
        if (exhibition != null) {
            // 增加浏览次数
            exhibitionService.increaseViewCount(id);
            return ResponseEntity.ok(exhibition);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 创建展览
     */
    @PostMapping
    public ResponseEntity<Exhibition> createExhibition(@RequestBody Exhibition exhibition) {
        return ResponseEntity.ok(exhibitionService.save(exhibition));
    }
    
    /**
     * 更新展览
     */
    @PutMapping("/{id}")
    public ResponseEntity<Exhibition> updateExhibition(@PathVariable Long id, @RequestBody Exhibition exhibition) {
        exhibition.setId(id);
        Exhibition updatedExhibition = exhibitionService.update(exhibition);
        if (updatedExhibition != null) {
            return ResponseEntity.ok(updatedExhibition);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除展览
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExhibition(@PathVariable Long id) {
        exhibitionService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 分页查询展览列表
     */
    @GetMapping("/getAllExhibitions")
    public ResponseEntity<Map<String, Object>> getExhibitions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String startTimeStr,
            @RequestParam(required = false) String endTimeStr,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = Sort.by(direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // 解析日期字符串
        Date startTime = null;
        Date endTime = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        try {
            if (startTimeStr != null && !startTimeStr.isEmpty()) {
                startTime = dateFormat.parse(startTimeStr);
            }
            if (endTimeStr != null && !endTimeStr.isEmpty()) {
                endTime = dateFormat.parse(endTimeStr);
            }
        } catch (ParseException e) {
            // 日期解析错误，记录日志
            System.err.println("日期解析错误: " + e.getMessage());
        }
        
        Page<Exhibition> exhibitionPage;
        if (name != null || type != null || location != null || startTime != null || endTime != null || status != null) {
            // 使用修改后的status字段类型
            exhibitionPage = exhibitionService.findByConditions(name, type, location, startTime, endTime, status, pageable);
        } else {
            exhibitionPage = exhibitionService.findAll(pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", exhibitionPage.getContent());
        response.put("currentPage", exhibitionPage.getNumber());
        response.put("totalItems", exhibitionPage.getTotalElements());
        response.put("totalPages", exhibitionPage.getTotalPages());
        
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
    
    /**
     * 获取当前正在进行的展览
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentExhibitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Exhibition> exhibitionPage = exhibitionService.findCurrentExhibitions(pageable);
        Map<String, Object> response = new HashMap<>();
        response.put("content", exhibitionPage.getContent());
        response.put("currentPage", exhibitionPage.getNumber());
        response.put("totalItems", exhibitionPage.getTotalElements());
        response.put("totalPages", exhibitionPage.getTotalPages());
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
    /**
     * 获取即将开始的展览
     */
    @GetMapping("/upcoming")
    public ResponseEntity<Map<String, Object>> getUpcomingExhibitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Exhibition> exhibitionPage = exhibitionService.findUpcomingExhibitions(pageable);
        Map<String, Object> response = new HashMap<>();
        response.put("content", exhibitionPage.getContent());
        response.put("currentPage", exhibitionPage.getNumber());
        response.put("totalItems", exhibitionPage.getTotalElements());
        response.put("totalPages", exhibitionPage.getTotalPages());
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
    /**
     * 获取已结束的展览
     */
    @GetMapping("/past")
    public ResponseEntity<Map<String, Object>> getPastExhibitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Exhibition> exhibitionPage = exhibitionService.findPastExhibitions(pageable);
        Map<String, Object> response = new HashMap<>();
        response.put("content", exhibitionPage.getContent());
        response.put("currentPage", exhibitionPage.getNumber());
        response.put("totalItems", exhibitionPage.getTotalElements());
        response.put("totalPages", exhibitionPage.getTotalPages());
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
    
    /**
     * 更新展览状态
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Exhibition> updateExhibitionStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        
        Exhibition exhibition = exhibitionService.updateStatus(id, status);
        if (exhibition != null) {
            return ResponseEntity.ok(exhibition);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取所有展览地点
     */
    @GetMapping("/locations")
    public ResponseEntity<List<String>> getAllLocations() {
        return ResponseEntity.ok(exhibitionService.findAllLocations());

    }
    
    /**
     * 获取所有展览主题
     */
    @GetMapping("/themes")
    public ResponseEntity<List<String>> getAllThemes() {
        return ResponseEntity.ok(exhibitionService.findAllThemes());
    }
    
    /**
     * 根据策展人查询展览
     */
    @GetMapping("/by-curator")
    public ResponseEntity<Map<String, Object>> getExhibitionsByCurator(
            @RequestParam String curator,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Exhibition> exhibitionPage = exhibitionService.findByCurator(curator, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", exhibitionPage.getContent());
        response.put("currentPage", exhibitionPage.getNumber());
        response.put("totalItems", exhibitionPage.getTotalElements());
        response.put("totalPages", exhibitionPage.getTotalPages());
        
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
    
    /**
     * 获取最受欢迎的展览
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularExhibitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Exhibition> exhibitionPage = exhibitionService.findMostViewed(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", exhibitionPage.getContent());
        response.put("currentPage", exhibitionPage.getNumber());
        response.put("totalItems", exhibitionPage.getTotalElements());
        response.put("totalPages", exhibitionPage.getTotalPages());
        
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", response);
        return ResponseEntity.ok(wrapper);
    }
}