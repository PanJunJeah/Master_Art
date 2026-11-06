package com.pan.master_art.agent.controller;

import com.pan.master_art.agent.dto.AgentChatRequest;
import com.pan.master_art.agent.dto.AgentChatResponse;
import com.pan.master_art.agent.orchestration.CuratorAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能策展 Agent 控制器
 * - POST /api/agent/chat        非流式（JSON，含草稿卡片等结构化产物）
 * - POST /api/agent/chat/stream SSE 流式（token 事件 + done 事件携带结构化产物）
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private CuratorAgentService curatorAgentService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody AgentChatRequest request) {
        AgentChatResponse response = curatorAgentService.chat(request);
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", response);
        return body;
    }

    /**
     * 清空当前用户对话记忆（前端「新对话」按钮）
     */
    @PostMapping("/chat/clear")
    public Map<String, Object> clearChat(@RequestBody AgentChatRequest request) {
        curatorAgentService.clearMemory(request.getUserId());
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "已开启新对话");
        return body;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody AgentChatRequest request) {
        return curatorAgentService.chatStreamSse(request);
    }
}
