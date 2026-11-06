package com.pan.master_art.agent.rag;

import com.pan.master_art.entity.Artwork;
import com.pan.master_art.repository.ArtworkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 基于 MySQL 的轻量 RAG 检索器：
 * 1. 关键词召回：对作品名/描述/故事/流派/艺术家名做 LIKE 召回；
 * 2. 相关性打分：按字段权重与词元命中数排序；
 * 3. 超时控制：检索超过 agent.rag.timeout-ms 抛出 KnowledgeRetrievalTimeoutException，
 *    由工具层降级为友好提示（对应测试场景 c）。
 *
 * 生产环境可无缝替换为向量库（Milvus/PGVector）+ Embedding 的语义检索实现。
 */
@Component
public class DatabaseKnowledgeRetriever implements KnowledgeRetriever {

    private static final Logger log = LoggerFactory.getLogger(DatabaseKnowledgeRetriever.class);

    private final ArtworkRepository artworkRepository;
    private final int timeoutMs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rag-retriever");
        t.setDaemon(true);
        return t;
    });

    public DatabaseKnowledgeRetriever(ArtworkRepository artworkRepository,
                                      @Value("${agent.rag.timeout-ms:2500}") int timeoutMs) {
        this.artworkRepository = artworkRepository;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public List<KnowledgeDocument> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String kw = query.trim();
        int k = Math.max(1, Math.min(topK, 10));

        // 提取关键词（中英文混合的朴素分词：按标点/空白切分 + 全串）
        List<String> tokens = tokenize(kw);
        tokens.add(kw);

        Future<List<KnowledgeDocument>> future = executor.submit(() -> doRetrieve(kw, tokens, k));
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            log.warn("RAG 检索超时（>{}ms），query={}", timeoutMs, kw);
            throw new KnowledgeRetrievalTimeoutException("知识库检索超时（" + timeoutMs + "ms）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KnowledgeRetrievalException("知识库检索被中断", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new KnowledgeRetrievalException("知识库检索失败：" + e.getCause().getMessage(), e.getCause());
        }
    }

    private List<KnowledgeDocument> doRetrieve(String kw, List<String> tokens, int topK) {
        // 关键修复：整句提问不是任何字段的连续子串，不能整句 LIKE。
        // 改为按词元（全词 + 中文二元组）逐个召回并合并去重，多词元命中即召回。
        java.util.Map<Long, Artwork> candidateMap = new java.util.LinkedHashMap<>();
        try {
            for (String token : tokens) {
                if (token == null || token.isBlank() || token.length() < 2) {
                    continue;
                }
                for (Artwork a : artworkRepository.findRagCandidates(token.trim())) {
                    candidateMap.putIfAbsent(a.getId(), a);
                }
                if (candidateMap.size() >= topK * 3) {
                    break; // 候选已充足
                }
            }
        } catch (Exception e) {
            throw new KnowledgeRetrievalException("知识库查询失败：" + e.getMessage(), e);
        }

        List<Artwork> candidates = new ArrayList<>(candidateMap.values());
        if (candidates.isEmpty()) {
            // 无关键词命中时，退化为浏览权重最高的在展作品，避免"知识库为空"的尴尬
            // 注意：使用 fetch join 的 searchForAgent，避免懒加载异常
            try {
                candidates = artworkRepository.searchForAgent(null, null, null, null, null,
                        org.springframework.data.domain.PageRequest.of(0, topK)).getContent();
            } catch (Exception e) {
                log.warn("RAG 兜底查询失败：{}", e.getMessage());
                candidates = List.of();
            }
        }

        List<KnowledgeDocument> docs = new ArrayList<>();
        for (Artwork a : candidates) {
            double score = score(a, tokens);
            if (score <= 0 && candidates.size() <= topK) {
                score = 0.1; // 兜底时给基础分
            }
            docs.add(new KnowledgeDocument(
                    "artwork-" + a.getId(),
                    a.getName(),
                    buildContent(a),
                    score,
                    a.getId()));
        }
        docs.sort(Comparator.comparingDouble(KnowledgeDocument::getScore).reversed());
        return docs.subList(0, Math.min(topK, docs.size()));
    }

    /**
     * 相关性打分：字段权重 × 命中数
     * 名称=5，流派/艺术家=3，故事/描述/历史意义/特点=1
     */
    private double score(Artwork a, List<String> tokens) {
        double score = 0;
        String name = lower(a.getName()) + " " + lower(a.getEnglishName());
        String genre = lower(a.getGenre());
        String artist = a.getArtist() == null ? "" : lower(a.getArtist().getName());
        String body = lower(a.getStory()) + " " + lower(a.getDescription())
                + " " + lower(a.getHistoricalSignificance()) + " " + lower(a.getFeatures());

        for (String token : tokens) {
            String t = lower(token);
            if (t.length() < 2) continue;
            if (name.contains(t)) score += 5;
            if (genre.contains(t)) score += 3;
            if (artist.contains(t)) score += 3;
            if (body.contains(t)) score += 1;
        }
        return score;
    }

    private String buildContent(Artwork a) {
        StringBuilder sb = new StringBuilder();
        sb.append("作品：《").append(a.getName()).append("》");
        if (a.getEnglishName() != null && !a.getEnglishName().isBlank()) {
            sb.append("（").append(a.getEnglishName()).append("）");
        }
        sb.append("；艺术家：").append(a.getArtist() == null ? "佚名" : a.getArtist().getName());
        if (a.getCreationYear() != null) sb.append("；年代：").append(a.getCreationYear());
        if (a.getGenre() != null) sb.append("；流派：").append(a.getGenre());
        if (a.getMaterial() != null) sb.append("；材质：").append(a.getMaterial());
        if (a.getStory() != null && !a.getStory().isBlank()) sb.append("；故事：").append(trim(a.getStory(), 120));
        if (a.getDescription() != null && !a.getDescription().isBlank()) sb.append("；描述：").append(trim(a.getDescription(), 120));
        if (a.getHistoricalSignificance() != null && !a.getHistoricalSignificance().isBlank())
            sb.append("；历史意义：").append(trim(a.getHistoricalSignificance(), 100));
        return sb.toString();
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String part : text.split("[\\s，。！？、；：,.!?;:()（）\\[\\]\"']+")) {
            if (!part.isBlank() && part.length() >= 2) {
                tokens.add(part);
                // 中文按二元组分片，提高召回
                if (part.length() > 2 && part.matches(".*[\\u4e00-\\u9fa5].*")) {
                    for (int i = 0; i < part.length() - 1; i++) {
                        tokens.add(part.substring(i, i + 2));
                    }
                }
            }
        }
        return new ArrayList<>(new HashSet<>(tokens));
    }

    private String lower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private String trim(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
