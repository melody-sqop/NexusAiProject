package com.cmt.NexusAi.ai.exception;

/**
 * AI审核异常基类
 */
public class AiAuditException extends RuntimeException {

    // 是否可重试
    private final boolean retryable;

    public AiAuditException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public AiAuditException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}