package com.pan.master_art.agent.rag;

import java.util.List;

/**
 * 知识检索器接口（RAG）
 * 生产实现：DatabaseKnowledgeRetriever（MySQL 关键词召回 + 打分 + 超时控制）
 * 可平滑替换为基于向量数据库的语义检索实现（例如 Spring AI VectorStore）。
 */
public interface KnowledgeRetriever {

    /**
     * 按查询语句检索最相关的知识片段
     *
     * @param query 检索问题
     * @param topK  返回条数（1-10）
     */
    List<KnowledgeDocument> retrieve(String query, int topK);
}
