package com.pan.master_art.exception;

/**
 * 业务异常：服务层/工具层以友好的中文提示抛给编排层或前端。
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
    }
}
