package com.cmt.yutumblike.ai.service;

import com.cmt.yutumblike.ai.constant.AiContentAuditConstant;
import com.cmt.yutumblike.model.AuditResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

// AiAuditService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAuditService {

    private final ChatClient.Builder chatClientBuilder;

    // Spring Retry拦截器捕获异常
    @Retryable(
            value = {RuntimeException.class, IllegalStateException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2) // 默认重试3次，每次间隔1秒
    )
    public boolean auditContent(String content) {
        log.info("[AI审核] 内容：{}", content);

        AuditResult result = chatClientBuilder.build()
                .prompt()
                .system(AiContentAuditConstant.AUDIT_SYSTEM_PROMPT)
                .user(content)
                .call()
                .entity(AuditResult.class);

        if (result == null || result.isPass() == null) {
            throw new IllegalStateException("AI返回结构缺失 isPass 字段");
        }

        log.info("[AI审核] 结果：isPass={}, reason={}, action={}",
                result.isPass(), result.reason(), result.action());

        return Boolean.TRUE.equals(result.isPass());
    }
}

