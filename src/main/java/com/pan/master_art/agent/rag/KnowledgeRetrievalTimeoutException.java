package com.pan.master_art.agent.rag;

/**
 * 知识检索超时异常 —— Agent 工具层据此降级为友好提示（测试场景 c 的触发点）
 */
public class KnowledgeRetrievalTimeoutException extends KnowledgeRetrievalException {

    public KnowledgeRetrievalTimeoutException(String message) {
        super(message);
    }

    public KnowledgeRetrievalTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
