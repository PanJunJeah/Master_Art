package com.pan.master_art.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pan.master_art.agent.orchestration.AgentTurnState;
import com.pan.master_art.dto.SessionView;
import com.pan.master_art.entity.Exhibition;
import com.pan.master_art.repository.ExhibitionRepository;
import com.pan.master_art.service.TicketSessionService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 展览只读工具（Agent 工具白名单成员）：
 * queryExhibition —— 查询展览排期、场次与余票。
 * 只读：不锁座、不扣库存。
 */
@Component
public class AgentExhibitionTools {

    private final ExhibitionRepository exhibitionRepository;
    private final TicketSessionService sessionService;
    private final ObjectMapper objectMapper;

    public AgentExhibitionTools(ExhibitionRepository exhibitionRepository,
                                TicketSessionService sessionService,
                                ObjectMapper objectMapper) {
        this.exhibitionRepository = exhibitionRepository;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "queryExhibition",
            description = "查询展览排期、场次时间与余票情况。可传展览ID查单个展览，或传名称关键词搜索，" +
                    "都不传时返回近期在售的进行中/即将开始的展览概要。回答预约/购票类问题前应先用本工具核实余票。")
    public String queryExhibition(
            @ToolParam(required = false, description = "展览ID（优先于名称）") Long exhibitionId,
            @ToolParam(required = false, description = "展览名称关键词") String name,
            ToolContext toolContext) {
        AgentTurnState state = AgentToolSupport.stateOf(toolContext);
        try {
            if (exhibitionId != null) {
                return detailOf(exhibitionId, state);
            }
            List<Exhibition> list;
            if (name != null && !name.isBlank()) {
                Page<Exhibition> page = exhibitionRepository.findByNameContainingOrEnglishNameContaining(
                        name.trim(), name.trim(), PageRequest.of(0, 5));
                list = page.getContent();
            } else {
                List<Exhibition> merged = new ArrayList<>();
                Page<Exhibition> ongoing = exhibitionRepository.findCurrentExhibitions(
                        new Date(), Exhibition.ExhibitionStatus.ONGOING, PageRequest.of(0, 5));
                Page<Exhibition> upcoming = exhibitionRepository.findUpcomingExhibitions(
                        new Date(), Exhibition.ExhibitionStatus.UPCOMING, PageRequest.of(0, 5));
                merged.addAll(ongoing.getContent());
                merged.addAll(upcoming.getContent());
                list = merged;
            }
            if (list.isEmpty()) {
                AgentToolSupport.trace(state, "queryExhibition", true, "未命中");
                return "暂时没有查询到相关展览。";
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (Exhibition e : list) {
                List<SessionView> sessions = sessionService.sessionsOf(e.getId(), true);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("exhibitionId", e.getId());
                item.put("name", e.getName());
                item.put("theme", e.getTheme());
                item.put("location", e.getLocation());
                item.put("startTime", format(e.getStartTime()));
                item.put("endTime", format(e.getEndTime()));
                item.put("status", e.getStatus() == null ? null : e.getStatus().name());
                item.put("visitInfo", AgentToolSupport.abbreviate(e.getVisitInfo(), 120));
                item.put("sessions", sessions);
                items.add(item);
            }
            AgentToolSupport.trace(state, "queryExhibition", true, "命中 " + items.size() + " 个展览");
            return "查询到以下展览及在售场次（含余票）：\n" + objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            AgentToolSupport.trace(state, "queryExhibition", false, "查询失败");
            return "展览查询失败：" + e.getMessage() + "。请向用户说明暂时无法查询展览信息。";
        }
    }

    private String detailOf(Long exhibitionId, AgentTurnState state) throws Exception {
        Exhibition e = exhibitionRepository.findById(exhibitionId).orElse(null);
        if (e == null) {
            AgentToolSupport.trace(state, "queryExhibition", true, "展览不存在");
            return "展览不存在：id=" + exhibitionId + "。请核对展览ID后重试。";
        }
        List<SessionView> sessions = sessionService.sessionsOf(exhibitionId, true);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("exhibitionId", e.getId());
        item.put("name", e.getName());
        item.put("theme", e.getTheme());
        item.put("location", e.getLocation());
        item.put("startTime", format(e.getStartTime()));
        item.put("endTime", format(e.getEndTime()));
        item.put("status", e.getStatus() == null ? null : e.getStatus().name());
        item.put("visitInfo", e.getVisitInfo());
        item.put("sessions", sessions);
        AgentToolSupport.trace(state, "queryExhibition", true, "展览详情 + " + sessions.size() + " 个场次");
        return "展览《" + e.getName() + "》的排期与余票如下：\n" + objectMapper.writeValueAsString(item);
    }

    private String format(Date date) {
        return date == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }
}
