package com.pan.master_art.controller;

import com.pan.master_art.dto.DraftView;
import com.pan.master_art.dto.ReservationView;
import com.pan.master_art.service.ReservationDraftService;
import com.pan.master_art.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 真实预约控制器 —— 人工确认闸门的"真实写入侧"
 *
 * 安全边界：这些接口只面向前端用户，【绝不】注册为 Agent 工具。
 * 用户看到 AI 生成的预约草稿卡片后，人工点击"确认预约"，
 * Vue3 直接调用 POST /api/reservations/confirm 完成真实写入与库存扣减。
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationDraftService draftService;

    /**
     * 人工确认预约草稿 → 生成真实预约 + 原子扣减库存
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@RequestBody Map<String, Object> body) {
        Long draftId = toLong(body.get("draftId"));
        Long userId = toLong(body.get("userId"));
        ReservationView view = reservationService.confirm(draftId, userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "预约成功");
        resp.put("data", view);
        return ResponseEntity.ok(resp);
    }

    /**
     * 查询草稿详情（前端渲染草稿卡片）
     */
    @GetMapping("/drafts/{id}")
    public ResponseEntity<Map<String, Object>> getDraft(@PathVariable Long id) {
        DraftView view = draftService.getView(id);
        Map<String, Object> resp = new HashMap<>();
        if (view == null) {
            resp.put("success", false);
            resp.put("message", "草稿不存在");
            return ResponseEntity.status(404).body(resp);
        }
        resp.put("success", true);
        resp.put("data", view);
        return ResponseEntity.ok(resp);
    }

    /**
     * 取消草稿（不占库存，直接作废）
     */
    @PostMapping("/drafts/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelDraft(@PathVariable Long id,
                                                           @RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        DraftView view = draftService.cancel(id, userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "草稿已取消");
        resp.put("data", view);
        return ResponseEntity.ok(resp);
    }

    /**
     * 我的草稿列表（前端「我的预约」页：可续确认/取消待确认草稿）
     */
    @GetMapping("/drafts/my")
    public ResponseEntity<Map<String, Object>> myDrafts(@RequestParam Long userId) {
        List<DraftView> list = draftService.listViewsByUser(userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", list);
        return ResponseEntity.ok(resp);
    }

    /**
     * 我的预约列表
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> myReservations(@RequestParam Long userId) {
        List<ReservationView> list = reservationService.listByUser(userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", list);
        return ResponseEntity.ok(resp);
    }

    /**
     * 取消已确认的预约（回补库存）
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelReservation(@PathVariable Long id,
                                                                 @RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        ReservationView view = reservationService.cancelReservation(id, userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "预约已取消，票额已回补");
        resp.put("data", view);
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
