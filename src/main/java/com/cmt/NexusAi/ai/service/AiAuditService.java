package com.cmt.NexusAi.ai.service;

import com.cmt.NexusAi.ai.constant.AiContentAuditConstant;
import com.cmt.NexusAi.ai.consumer.AiAuditCoreService;
import com.cmt.NexusAi.ai.exception.AiNonRetryableException;
import com.cmt.NexusAi.ai.exception.AiRetryableException;
import com.cmt.NexusAi.model.AuditResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

/**
 * AI审核服务入口 用AOP代理调用核心审核逻辑 此入口还可以增加额外的统一逻辑：
 *      统一异常处理、统一日志、统一限流、统一熔断、统一降级、统一监控等
 */
@Slf4j
@Service
public class AiAuditService {


    @Resource
    private AiAuditCoreService coreService;

    /**
     * 对外暴露的审核入口
     * 只重试"可重试异常"，格式错误直接转人工
     */
    //TODO 后期可以在这里加限流 防止而已调用api接口
    public boolean auditContent(String content) {
        try {
            return coreService.doAudit(content);
        } catch (AiNonRetryableException e) {
            // 不可重试错误：直接返回false（不通过），让上层转人工
            log.warn("[AI审核] 不可重试错误，直接转人工：{}", e.getMessage());
            return false;  // 返回false，消费者会把它当成"不通过"处理，最终转人工
        }
    }




}

