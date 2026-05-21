package com.cmt.NexusAi.modules.audit.L2b.service;

import com.cmt.NexusAi.modules.audit.L2b.enums.AuditModelLevel;
import com.cmt.NexusAi.modules.audit.L2b.model.DTO.AuditContextDTO;
import com.cmt.NexusAi.modules.audit.L2b.model.vo.AuditResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncAiAuditService {

    private final AiAuditCoreService aiAuditCoreService;

    @Value("${audit.sync.timeout-ms:300}")
    private long syncTimeoutMs;

    private final ExecutorService aiAuditExecutor = new ThreadPoolExecutor(
            200, 200, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5000),
            r -> {
                Thread t = new Thread(r, "sync-ai-audit");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // 1.3 核心改造：入参由 String content 替换为 AuditContext
    public AuditResult auditWithTimeout(AuditContextDTO context) {
        CompletableFuture<AuditResult> future = CompletableFuture.supplyAsync(
                () -> aiAuditCoreService.doAudit(context, AuditModelLevel.MEDIUM, false),
                aiAuditExecutor
        );

        try {
            return future.get(syncTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("[同步AI] 审核超时(>{}ms)，降级为PENDING异步兜底", syncTimeoutMs);
            return AuditResult.degrade("同步AI超时降级");
        } catch (Exception e) {
            log.error("[同步AI] 审核异常，降级为PENDING异步兜底", e);
            return AuditResult.degrade("同步AI异常降级");
        }
    }
}