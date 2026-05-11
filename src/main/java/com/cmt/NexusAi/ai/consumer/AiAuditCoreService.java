package com.cmt.NexusAi.ai.consumer;

import com.cmt.NexusAi.ai.constant.AiContentAuditConstant;
import com.cmt.NexusAi.ai.exception.AiNonRetryableException;
import com.cmt.NexusAi.ai.exception.AiRetryableException;
import com.cmt.NexusAi.model.AuditResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.shade.org.jvnet.hk2.annotations.Service;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiAuditCoreService {  // 核心审核，带重试

    private final ChatClient.Builder chatClientBuilder;

    /**
     * 真正的审核逻辑，带重试
     * 只有 AiRetryableException 会触发重试
     */
    @Retryable(
            value = {AiRetryableException.class},  // ← 只重试这个异常
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean doAudit(String content) {
        log.info("[AI审核] 内容：{}", content);

        // ========== 第1步：调AI ==========
        String raw;
        try {
            raw = chatClientBuilder.build()
                    .prompt()
                    .system(AiContentAuditConstant.AUDIT_SYSTEM_PROMPT)
                    .user(content)
                    .call()
                    .content();
        } catch (Exception e) {
            // 网络超时、连接失败 → 可重试
            if (isNetworkError(e)) {
                log.warn("[AI审核] 网络错误，准备重试：{}", e.getMessage());
                throw new AiRetryableException("AI服务网络异常", e);
            }
            // 其他异常（如400参数错误）→ 不可重试
            log.error("[AI审核] 调用异常，不重试：{}", e.getMessage());
            throw new AiNonRetryableException("AI调用失败：" + e.getMessage());
        }

        log.info("[AI审核] 原始返回：{}", raw);

        // ========== 第2步：清洗 ==========
        String cleaned = clean(raw);
        if (cleaned.isEmpty()) {
            log.warn("[AI审核] 清洗后为空，原始返回：{}", raw);
            throw new AiNonRetryableException("AI返回格式异常，清洗后为空");
        }
        log.info("[AI审核] 清洗后：{}", cleaned);

        // ========== 第3步：Schema校验 ==========
        if (!checkSchema(cleaned)) {
            log.warn("[AI审核] Schema校验失败：{}", cleaned);
            throw new AiNonRetryableException("Schema校验失败");
        }

        // ========== 第4步：解析 ==========
        AuditResult result;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            result = mapper.readValue(cleaned, AuditResult.class);
        } catch (Exception e) {
            log.error("[AI审核] JSON解析失败：{}", cleaned, e);
            throw new AiNonRetryableException("JSON解析失败");
        }

        // ========== 第5步：业务校验 ==========
        if (result == null || result.isPass() == null || result.action() == null) {
            log.warn("[AI审核] 业务字段缺失：{}", result);
            throw new AiNonRetryableException("业务字段缺失");
        }

        log.info("[AI审核] 结果：isPass={}, action={}", result.isPass(), result.action());
        return Boolean.TRUE.equals(result.isPass());
    }


    /**
     * 判断是否是网络错误（可重试）
     */
    private boolean isNetworkError(Exception e) {
        return e instanceof SocketTimeoutException
                || e instanceof ResourceAccessException
                || (e.getMessage() != null && (
                e.getMessage().contains("timeout")
                        || e.getMessage().contains("Connection refused")
                        || e.getMessage().contains("503")
                        || e.getMessage().contains("429")
        ));
    }

    // 清洗：只干三件事
    private String clean(String raw) {
        if (raw == null) return "";

        // 1. 去掉 ```json 和 ```
        raw = raw.replace("```json", "").replace("```", "");

        // 2. 去掉前后空格
        raw = raw.trim();

        // 3. 只保留 { 到 } 之间的内容
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) return "";

        return raw.substring(start, end + 1);
    }

    // Schema校验：只检查三个东西
    private boolean checkSchema(String json) {
        // 1. 是不是合法JSON
        try {
            new ObjectMapper().readTree(json);
        } catch (Exception e) {
            return false;
        }

        // 2. 有没有 isPass
        if (!json.contains("\"isPass\"")) return false;

        // 3. 有没有 action，且值对不对
        if (!json.contains("\"action\"")) return false;

        return true;
    }
}

