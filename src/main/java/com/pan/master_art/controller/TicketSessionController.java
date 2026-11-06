package com.pan.master_art.controller;

import com.pan.master_art.dto.SessionView;
import com.pan.master_art.service.TicketSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 展览排期（场次）控制器 —— 只读，供前端票务区展示排期与余票
 */
@RestController
@RequestMapping("/api/ticket-sessions")
public class TicketSessionController {

    @Autowired
    private TicketSessionService sessionService;

    /**
     * 某展览的场次与余票（onSaleOnly=true 时仅返回在售场次）
     */
    @GetMapping("/exhibition/{exhibitionId}")
    public ResponseEntity<Map<String, Object>> sessions(@PathVariable Long exhibitionId,
                                                        @RequestParam(defaultValue = "false") boolean onSaleOnly) {
        List<SessionView> list = sessionService.sessionsOf(exhibitionId, onSaleOnly);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", list);
        return ResponseEntity.ok(resp);
    }
}
