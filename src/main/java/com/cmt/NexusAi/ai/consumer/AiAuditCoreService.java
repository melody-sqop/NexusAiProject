package com.cmt.NexusAi.ai.consumer;

import com.cmt.NexusAi.ai.constant.AiContentAuditConstant;
import com.cmt.NexusAi.ai.exception.AiNonRetryableException;
import com.cmt.NexusAi.ai.exception.AiRetryableException;
import com.cmt.NexusAi.model.AuditResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAuditCoreService {

    private final ChatClient.Builder chatClientBuilder;
    private JsonSchema jsonSchema;

    /**
     * 初始化时加载JSON Schema
     *
     * 为什么放在@PostConstruct：服务启动时预加载，避免每次审核都读文件
     * 不这样做：每次审核都new一个Schema对象，浪费内存和CPU
     */
    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/audit-result-schema.json")) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
            jsonSchema = factory.getSchema(is);
            log.info("[AI审核] JSON Schema加载成功");
        } catch (Exception e) {
            log.error("[AI审核] JSON Schema加载失败", e);
            throw new RuntimeException("JSON Schema加载失败", e);
        }
    }

    @Retryable(
            value = {AiRetryableException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public AuditResult doAudit(String content) {
        log.info("[AI审核] 内容：{}", content);

        String raw;
        try {
            raw = chatClientBuilder.build()
                    .prompt()
                    .system(AiContentAuditConstant.AUDIT_SYSTEM_PROMPT)
                    .user(content)
                    .call()
                    .content();
        } catch (Exception e) {
            if (isNetworkError(e)) {
                log.warn("[AI审核] 网络错误，准备重试：{}", e.getMessage());
                throw new AiRetryableException("AI服务网络异常", e);
            }
            log.error("[AI审核] 调用异常，不重试：{}", e.getMessage());
            throw new AiNonRetryableException("AI调用失败：" + e.getMessage());
        }

        log.info("[AI审核] 原始返回：{}", raw);

        String cleaned = clean(raw);
        if (cleaned.isEmpty()) {
            log.warn("[AI审核] 清洗后为空，原始返回：{}", raw);
            throw new AiNonRetryableException("AI返回格式异常，清洗后为空");
        }
        log.info("[AI审核] 清洗后：{}", cleaned);

        // 【核心】JSON Schema校验
        if (!checkSchema(cleaned)) {
            log.warn("[AI审核] Schema校验失败：{}", cleaned);
            throw new AiNonRetryableException("Schema校验失败");
        }

        AuditResult result;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            result = mapper.readValue(cleaned, AuditResult.class);
        } catch (Exception e) {
            log.error("[AI审核] JSON解析失败：{}", cleaned, e);
            throw new AiNonRetryableException("JSON解析失败");
        }

        if (result == null || result.isPass() == null || result.action() == null) {
            log.warn("[AI审核] 业务字段缺失：{}", result);
            throw new AiNonRetryableException("业务字段缺失");
        }

        log.info("[AI审核] 结果：isPass={}, action={}", result.isPass(), result.action());
        return result;
    }

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

    private String clean(String raw) {
        if (raw == null) return "";
        raw = raw.replace("```json", "").replace("```", "");
        raw = raw.trim();
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) return "";
        return raw.substring(start, end + 1);
    }

    /**
     * JSON Schema校验（替换原来的字符串包含判断）
     *
     * 为什么要用Schema：精确校验字段类型、枚举值、条件逻辑
     * 不这样做：模型返回 {"isPass":true, "action":"block"} 这种矛盾数据，业务逻辑会崩溃
     */
    private boolean checkSchema(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(json);

            Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);
            if (!errors.isEmpty()) {
                log.warn("[AI审核] Schema校验错误：{}", errors);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("[AI审核] Schema校验异常", e);
            return false;
        }
    }
}