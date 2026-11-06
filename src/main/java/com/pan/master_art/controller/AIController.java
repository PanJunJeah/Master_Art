package com.pan.master_art.controller;

import com.pan.master_art.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AIController {
    @Autowired
    private AIService aiService;

    @PostMapping("/chat")
    public String chatEndpoint(@RequestBody String userMessage) {
        try {
            return aiService.getAiResponse(userMessage);
        } catch (Exception e) {
            return "请求失败: " + e.getMessage();
        }
    }
}