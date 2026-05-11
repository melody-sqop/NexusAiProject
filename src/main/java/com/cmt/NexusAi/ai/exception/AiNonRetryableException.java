package com.cmt.NexusAi.ai.exception;

/**
 * 不可重试异常：格式错误、参数错误等
 */
public class AiNonRetryableException extends AiAuditException {
    public AiNonRetryableException(String message) {
        super(message, false);
    }
}
