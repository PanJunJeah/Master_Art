package com.pan.master_art.controller;

import com.pan.master_art.entity.ExhibitionSubscription;
import com.pan.master_art.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新展订阅控制器 —— 前端订阅按钮（Agent 工具 createSubscription 复用同一服务）
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        Long exhibitionId = toLong(body.get("exhibitionId"));
        String message = subscriptionService.subscribe(userId, exhibitionId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        Long exhibitionId = toLong(body.get("exhibitionId"));
        String message = subscriptionService.unsubscribe(userId, exhibitionId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> mySubscriptions(@RequestParam Long userId) {
        // 返回事务内组装的视图（避免懒加载序列化异常）
        List<com.pan.master_art.dto.SubscriptionView> list = subscriptionService.listViewsByUser(userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", list);
        return ResponseEntity.ok(resp);
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
