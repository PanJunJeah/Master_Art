package com.pan.master_art.controller;

import com.pan.master_art.entity.Exhibition;
import com.pan.master_art.repository.ArtistRepository;
import com.pan.master_art.repository.ArtworkRepository;
import com.pan.master_art.repository.ExhibitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 平台数据看板（首页统计）
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ExhibitionRepository exhibitionRepository;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("artworks", artworkRepository.countByStatus(1));
        data.put("artists", artistRepository.countByStatus(1));
        data.put("exhibitions", exhibitionRepository.count());
        data.put("ongoingExhibitions", exhibitionRepository.countByStatus(Exhibition.ExhibitionStatus.ONGOING));
        data.put("upcomingExhibitions", exhibitionRepository.countByStatus(Exhibition.ExhibitionStatus.UPCOMING));
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }
}
