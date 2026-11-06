package com.pan.master_art.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pan.master_art.agent.orchestration.AgentTurnState;
import com.pan.master_art.agent.rag.KnowledgeDocument;
import com.pan.master_art.agent.rag.KnowledgeRetrievalException;
import com.pan.master_art.agent.rag.KnowledgeRetrievalTimeoutException;
import com.pan.master_art.agent.rag.KnowledgeRetriever;
import com.pan.master_art.entity.Artwork;
import com.pan.master_art.repository.ArtworkRepository;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 艺术品只读工具集（Agent 工具白名单成员）：
 * - searchArtworkKnowledge：RAG 检索艺术品背景知识（超时降级为友好提示）
 * - searchArtworks：多条件检索在展艺术品
 */
@Component
public class AgentArtworkTools {

    private final ArtworkRepository artworkRepository;
    private final KnowledgeRetriever knowledgeRetriever;
    private final ObjectMapper objectMapper;

    public AgentArtworkTools(ArtworkRepository artworkRepository,
                             KnowledgeRetriever knowledgeRetriever,
                             ObjectMapper objectMapper) {
        this.artworkRepository = artworkRepository;
        this.knowledgeRetriever = knowledgeRetriever;
        this.objectMapper = objectMapper;
    }

    /**
     * 只读工具：RAG 检索艺术品背景知识
     */
    @Tool(name = "searchArtworkKnowledge",
            description = "从艺术知识库（RAG）检索艺术品的背景故事、创作故事、历史意义等知识。" +
                    "回答「这幅作品有什么故事/背景」类问题时必须使用本工具，不要凭记忆编造。")
    public String searchArtworkKnowledge(
            @ToolParam(required = true, description = "检索问题，例如「梵高 星空 的创作背景」") String query,
            @ToolParam(required = false, description = "返回条数，1-5，默认3") Integer topK,
            ToolContext toolContext) {
        AgentTurnState state = AgentToolSupport.stateOf(toolContext);
        int k = topK == null ? 3 : Math.max(1, Math.min(topK, 5));
        try {
            List<KnowledgeDocument> docs = knowledgeRetriever.retrieve(query, k);
            if (docs.isEmpty()) {
                AgentToolSupport.trace(state, "searchArtworkKnowledge", true, "未命中");
                return "知识库中暂时没有找到与「" + query + "」直接相关的内容，建议换个关键词或问法再试。";
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (KnowledgeDocument doc : docs) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", doc.getTitle());
                item.put("content", doc.getContent());
                item.put("score", Math.round(doc.getScore() * 100) / 100.0);
                items.add(item);
            }
            AgentToolSupport.trace(state, "searchArtworkKnowledge", true, "命中 " + docs.size() + " 条");
            return "知识库检索到以下背景资料（按相关度排序）：\n"
                    + objectMapper.writeValueAsString(items);
        } catch (KnowledgeRetrievalTimeoutException e) {
            // 降级：检索超时不抛异常，返回友好提示给模型
            AgentToolSupport.trace(state, "searchArtworkKnowledge", false, "检索超时");
            return "知识库检索超时，暂时无法获取「" + query + "」的背景资料。请向用户说明检索超时，建议稍后重试或换一种问法。";
        } catch (KnowledgeRetrievalException e) {
            AgentToolSupport.trace(state, "searchArtworkKnowledge", false, "检索失败");
            return "知识库检索失败：" + e.getMessage() + "。请向用户致歉并建议稍后重试。";
        } catch (Exception e) {
            AgentToolSupport.trace(state, "searchArtworkKnowledge", false, "未知异常");
            return "知识库检索出现异常：" + e.getMessage() + "。请向用户说明暂时无法检索知识库。";
        }
    }

    /**
     * 只读工具：多条件检索在展艺术品
     */
    @Tool(name = "searchArtworks",
            description = "按名称、流派、艺术家、类型、创作年代等多条件检索「在展/在售」艺术品。返回作品概要列表。")
    public String searchArtworks(
            @ToolParam(required = false, description = "作品名称关键词（中英文均可）") String name,
            @ToolParam(required = false, description = "艺术流派，如「印象派」「超现实主义」") String genre,
            @ToolParam(required = false, description = "艺术家姓名关键词") String artistName,
            @ToolParam(required = false, description = "作品类型：1-绘画 2-雕塑 3-装置 4-摄影 5-数字艺术 6-其他") Integer type,
            @ToolParam(required = false, description = "创作年代，如「1889」") String creationYear,
            @ToolParam(required = false, description = "返回数量，默认5，最大10") Integer limit,
            ToolContext toolContext) {
        AgentTurnState state = AgentToolSupport.stateOf(toolContext);
        int size = limit == null ? 5 : Math.max(1, Math.min(limit, 10));
        try {
            Page<Artwork> page = artworkRepository.searchForAgent(name, artistName, type, genre, creationYear,
                    PageRequest.of(0, size));
            List<Map<String, Object>> items = new ArrayList<>();
            for (Artwork a : page.getContent()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("artworkId", a.getId());
                item.put("name", a.getName());
                item.put("englishName", a.getEnglishName());
                item.put("artist", a.getArtist() == null ? "佚名" : a.getArtist().getName());
                item.put("genre", a.getGenre());
                item.put("type", a.getType());
                item.put("creationYear", a.getCreationYear());
                item.put("material", a.getMaterial());
                item.put("size", a.getSize());
                item.put("collectionPlace", a.getCollectionPlace());
                item.put("description", AgentToolSupport.abbreviate(a.getDescription(), 80));
                items.add(item);
            }
            AgentToolSupport.trace(state, "searchArtworks", true, "命中 " + items.size() + " 件");
            if (items.isEmpty()) {
                return "没有找到符合条件的艺术品，建议放宽条件（如去掉流派/年代限制）再试。";
            }
            return "共找到 " + page.getTotalElements() + " 件相关作品，展示前 " + items.size() + " 件：\n"
                    + objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            AgentToolSupport.trace(state, "searchArtworks", false, "查询失败");
            return "艺术品检索失败：" + e.getMessage() + "。请向用户说明暂时无法查询作品。";
        }
    }
}
