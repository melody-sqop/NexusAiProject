package com.cmt.NexusAi.modules.audit.L2b.comsumer;

import com.cmt.NexusAi.common.enums.ViolationTag;
import com.cmt.NexusAi.common.service.UserScoreService;
import com.cmt.NexusAi.modules.audit.L2a.service.ShadowAuditService;
import com.cmt.NexusAi.modules.audit.L2a.service.SimHashCacheService;
import com.cmt.NexusAi.modules.audit.L2b.model.vo.AuditResult;
import com.cmt.NexusAi.modules.audit.L2b.service.AiAuditService;
import com.cmt.NexusAi.modules.audit.L3.enums.ManualAuditNotify;
import com.cmt.NexusAi.modules.audit.L3.service.ManualAuditTaskService;
import com.cmt.NexusAi.modules.comment.model.entity.Comment;
import com.cmt.NexusAi.modules.comment.service.CommentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.cmt.NexusAi.modules.audit.L2b.constant.AiContentAuditConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = AUDIT_TOPIC,
        consumerGroup = "comment-audit-consumer-group"
)
public class AiContentAuditConsumer implements RocketMQListener<String> {

    private final CommentService commentService;
    private final RocketMQTemplate rocketMQTemplate;
    private final AiAuditService aiAuditService;
    private final ManualAuditTaskService manualAuditTaskService;
    private final UserScoreService userScoreService;

    @Autowired
    private SimHashCacheService simHashCacheService;
    @Autowired
    private ShadowAuditService shadowAuditService;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(String message) {
        Long commentId = null;
        String source = "UNKNOWN";
        Long userId = null;

        try {
            JsonNode node = objectMapper.readTree(message);

            if (!node.has("commentId")) {
                log.info("[审核消费] 影子采样消息，仅记录日志 | source={}",
                        node.has("source") ? node.get("source").asText() : "UNKNOWN");
                return;
            }

            commentId = node.get("commentId").asLong();
            source = node.has("source") ? node.get("source").asText() : "UNKNOWN";
            userId = node.has("userId") ? node.get("userId").asLong() : null;
        } catch (Exception e) {
            log.error("[审核消费] 消息解析失败: {}", message, e);
            return;
        }

        Comment comment = commentService.getById(commentId);
        if (comment == null) return;

        boolean isReportReview = "REJECTED".equals(comment.getAuditResult());

        if (!isReportReview) {
            boolean locked = commentService.lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .eq(Comment::getAuditResult, "PENDING")
                    .set(Comment::getAuditResult, "AUDITING")
                    .update();
            if (!locked) {
                log.warn("[审核消费] 评论[{}]已被其他消费者处理，跳过", commentId);
                return;
            }
            comment = commentService.getById(commentId);
        }

        doAudit(comment, isReportReview, source, userId);
    }

    private void doAudit(Comment comment, boolean isReportReview, String source, Long userId) {
        Long commentId = comment.getId();
        String content = comment.getContent();

        try {
            AuditResult result = aiAuditService.auditContent(content);
            String aiReason = result.reason();

            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║      [决策-MQ] 异步AI终审结果             ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║ 评论ID: " + commentId);
            System.out.println("║ 来源: " + source);
            System.out.println("║ AI原始action: " + result.action());
            System.out.println("║ AI原始reason: " + aiReason);
            System.out.println("╚══════════════════════════════════════════╝");

            switch (result.action()) {
                case "pass" -> {
                    commentService.lambdaUpdate()
                            .eq(Comment::getId, commentId)
                            .set(Comment::getAuditResult, "PASSED")
                            .set(Comment::getViolationTag, ViolationTag.PASS.getTag())
                            .update();
                    System.out.println("[决策-MQ] pass → 直接放行");
                }
                case "block" -> {
                    commentService.lambdaUpdate()
                            .eq(Comment::getId, commentId)
                            .set(Comment::getDisplayStatus, "HIDDEN")
                            .set(Comment::getAuditResult, "REJECTED")
                            .set(Comment::getViolationTag, ViolationTag.AI_REJECT.getTag())
                            .update();

                    simHashCacheService.cache(content, ViolationTag.AI_REJECT.getTag(), "AI_AUDIT", "REJECT", aiReason);
                    System.out.println("[决策-MQ] SimHash写入: 成功 | violationTag=AI_REJECT");

                    if (userId != null) {
                        userScoreService.addScore(userId, ViolationTag.AI_REJECT, commentId.toString());
                    }
                }
                case "review" -> {
                    commentService.lambdaUpdate()
                            .eq(Comment::getId, commentId)
                            .set(Comment::getAuditResult, "MANUAL_REVIEW")
                            .update();
                    manualAuditTaskService.saveWithRetry(commentId, content, aiReason);
                    notifyManualReview(commentId, content, aiReason);
                    System.out.println("[决策-MQ] review → 送人工复核 | 不写入SimHash");
                }
                default -> throw new IllegalStateException("未知 action: " + result.action());
            }

            if ("SHADOW_SAMPLE".equals(source) || "SIMHASH_SHADOW".equals(source)) {
                int aiLevel = convertToLevel(result);
                shadowAuditService.recordShadow(content, 999, aiLevel, !result.isPass());
                System.out.println("[决策-MQ] 影子日志: 已记录 | aiLevel=" + aiLevel);
            }

            log.info("[审核消费] 评论[{}]完成 | action={} | 来源={}",
                    commentId, result.action(), isReportReview ? "举报" : source);

        } catch (Exception e) {
            log.error("[审核消费] 评论[{}]异常，转人工", commentId, e);
            System.out.println("[决策-MQ] AI调用异常 → 降级转人工");
            commentService.lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .set(Comment::getAuditResult, "MANUAL_REVIEW")
                    .update();
            manualAuditTaskService.saveWithRetry(commentId, content, "异常：" + e.getMessage());
        }
    }

    // 改：不再引用 RiskLevel 枚举，直接写数字（兼容 shadowAuditService.recordShadow 的 int 入参）
    private int convertToLevel(AuditResult result) {
        return switch (result.reason()) {
            case "block" -> 3;   // 对应原 HIGH
            case "review" -> 2;  // 对应原 MEDIUM
            case "pass" -> 0;    // 对应原 SAFE
            default -> 2;
        };
    }

    private void notifyManualReview(Long commentId, String content, String aiReason) {
        try {
            ManualAuditNotify notify = new ManualAuditNotify(
                    commentId, content, aiReason, Instant.now().toString()
            );
            String notifyMsg = objectMapper.writeValueAsString(notify);
            rocketMQTemplate.asyncSend(MANUAL_NOTIFY_TOPIC, notifyMsg, new SendCallback() {
                @Override public void onSuccess(SendResult sendResult) {
                    log.info("[审核消费] 人工审核通知发送成功, commentId={}", commentId);
                }
                @Override public void onException(Throwable e) {
                    log.error("[审核消费] 人工审核通知发送失败 commentId={}", commentId, e);
                }
            });
        } catch (Exception ex) {
            log.error("[审核消费] 构建通知消息失败 commentId={}", commentId, ex);
        }
    }
}