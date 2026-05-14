package com.cmt.NexusAi.ai.service;

import com.cmt.NexusAi.ai.consumer.AiAuditCoreService;
import com.cmt.NexusAi.ai.exception.AiNonRetryableException;
import com.cmt.NexusAi.ai.exception.AiRetryableException;
import com.cmt.NexusAi.model.AuditResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI审核服务入口
 * 统一异常处理、统一日志，后期可扩展限流、熔断、降级
 */
@Slf4j
@Service
public class AiAuditService {

    @Resource
    private AiAuditCoreService coreService;

    /**
     * 对外暴露的审核入口
     *
     * 为什么要这样设计：
     * - 成功：返回 AI 审核结果（pass/block/review）
     * - 失败（网络异常）：@Retryable 自动重试，重试耗尽抛 AiRetryableException
     * - 失败（格式/Schema错误）：返回 review，让上层转人工，而不是直接驳回
     *
     * 不这样做：不可重试错误直接返回 block，正常内容被误杀
     */
    public AuditResult auditContent(String content) {
        try {
            return coreService.doAudit(content);
        } catch (AiNonRetryableException e) {
            // 不可重试错误：AI 输出异常，不是内容违规
            // 返回 review，让 Consumer 转人工复核，而不是直接 block
            log.warn("[AI审核] 不可重试错误，转人工复核：{}", e.getMessage());
            return new AuditResult(false, null, "AI解析失败：" + e.getMessage(), "review");
        } catch (AiRetryableException e) {
            // 重试耗尽，网络问题
            log.error("[AI审核] 重试耗尽，转人工复核：{}", e.getMessage());
            return new AuditResult(false, null, "AI服务不可用：" + e.getMessage(), "review");
        }
    }
}