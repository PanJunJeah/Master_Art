package com.pan.master_art.agent.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识文档（RAG 检索结果单元）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {
    /** 文档标识（如 artwork-1） */
    private String id;
    /** 标题（作品名） */
    private String title;
    /** 正文片段 */
    private String content;
    /** 相关度得分（越大越相关） */
    private double score;
    /** 来源业务ID（artwork id） */
    private Long sourceId;
}
