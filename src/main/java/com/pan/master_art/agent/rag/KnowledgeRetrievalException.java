package com.pan.master_art.agent.rag;

/**
 * 知识检索异常基类
 */
public class KnowledgeRetrievalException extends RuntimeException {

    public KnowledgeRetrievalException(String message) {
        super(message);
    }

    public KnowledgeRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
