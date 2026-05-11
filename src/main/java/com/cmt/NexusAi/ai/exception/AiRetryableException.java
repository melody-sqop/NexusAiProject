package com.cmt.NexusAi.ai.exception;

/**
 * 可重试异常：网络超时、服务不可用等
 */
public class AiRetryableException extends AiAuditException {
    public AiRetryableException(String message, Throwable cause) {
        super(message, cause, true);
    }
}
