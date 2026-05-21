package com.cmt.NexusAi.modules.audit.L2b.comsumer;

import com.cmt.NexusAi.common.enums.ViolationTag;
import com.cmt.NexusAi.common.service.UserScoreService;
import com.cmt.NexusAi.modules.audit.L2a.service.ShadowAuditService;
import com.cmt.NexusAi.modules.audit.L2a.service.SimHashCacheService;
import com.cmt.NexusAi.modules.audit.L2b.enums.AuditModelLevel;
import com.cmt.NexusAi.modules.audit.L2b.constant.AiContentAuditConstant;
import com.cmt.NexusAi.modules.audit.L2b.model.DTO.AuditContextDTO; // 💥 引入 v5.0 新增的 DTO
import com.cmt.NexusAi.modules.audit.L2b.model.DTO.AuditMessageDTO;
import com.cmt.NexusAi.modules.audit.L2b.model.vo.AuditResult;
import com.cmt.NexusAi.modules.audit.L2b.service.AiAuditCoreService;
import com.cmt.NexusAi.modules.audit.L3.enums.ManualAuditNotify;
import com.cmt.NexusAi.modules.audit.L3.service.ManualAuditTaskService;
import com.cmt.NexusAi.modules.comment.model.entity.Comment;
import com.cmt.NexusAi.modules.comment.service.CommentService;
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
import java.util.concurrent.ThreadLocalRandom;

import static com.cmt.NexusAi.modules.audit.L2b.constant.AiContentAuditConstant.AUDIT_TOPIC;

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
    private final AiAuditCoreService aiAuditCoreService;
    private final ManualAuditTaskService manualAuditTaskService;
    private final UserScoreService userScoreService;

    @Autowired private SimHashCacheService simHashCacheService;
    @Autowired private ShadowAuditService shadowAuditService;
    @Autowired private ObjectMapper objectMapper;

    /**
     * 普通评论的异步抽样比例 (10%)
     * TODO: 后续演进可抽到 Nacos 或 @Value 配置
     */
    private static final int ASYNC_SAMPLE_RATE = 10;

    @Override
    public void onMessage(String message) {
        AuditMessageDTO msg;
        try {
            msg = objectMapper.readValue(message, AuditMessageDTO.class);
        } catch (Exception e) {
            log.error("[审核消费] 消息解析失败: {}", message, e);
            return;
        }

        Long commentId = msg.getCommentId();
        if (commentId == null) {
            log.info("[审核消费] 影子采样纯日志消息，跳过 | source={}", msg.getSource());
            return;
        }

        Comment comment = commentService.getById(commentId);
        if (comment == null) return;

        // 幂等保障1：同步AI已经明确判违规(SELF_ONLY)或已通过(PASSED)的，异步直接丢弃，不浪费Token
        if ("REJECTED".equals(comment.getAuditResult()) || "PASSED".equals(comment.getAuditResult())) {
            log.info("[审核消费] 评论[{}]已被同步处理({})，异步丢弃 | source={}",
                    commentId, comment.getAuditResult(), msg.getSource());
            return;
        }

        // 幂等保障2：锁定状态，防止并发消费
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

        doAudit(comment, msg);
    }

    private void doAudit(Comment comment, AuditMessageDTO msg) {
        Long commentId = comment.getId();
        String content = comment.getContent();
        boolean needFullAudit = msg.isNeedFullAudit();
        boolean l1P1Hit = msg.isL1P1Hit();

        // 💥 v5.0 核心改造：组装上下文 DTO，实现零查库
        // 优先从 MQ 透传的快照取，若为空则降级从 DB 冗余字段取 (防历史消息丢失上下文)
        String scene = msg.getScene() != null ? msg.getScene() : "BLOG_COMMENT";
        String parentTitle = msg.getParentTitle() != null ? msg.getParentTitle() : comment.getParentTitle();

        AuditContextDTO contextDTO = AuditContextDTO.builder()
                .content(content)
                .scene(scene)
                .parentTitle(parentTitle)
                .build();

        try {
            AuditResult result = null;
            boolean isShadowSource = "SHADOW_SAMPLE".equals(msg.getSource()) || "SIMHASH_SHADOW".equals(msg.getSource());

            // ===== 核心路由逻辑：降本增效 + 异步级联 =====
            if (needFullAudit || isShadowSource) {
                // 路径1：高危/影子采样，100%全量审核，使用中等模型
                // 💥 修改：传入 contextDTO
                result = aiAuditCoreService.doAudit(contextDTO, AuditModelLevel.MEDIUM, false);
                // 💥 修改日志：加上 result.getModelName()
                log.info("[异步-MQ] 全量审核 | commentId={} | action={} | model={}", commentId, result.action(), result.modelName());
            } else {
                // 路径2：普通放行，10%抽样审核，使用廉价极简模型
                if (ThreadLocalRandom.current().nextInt(100) < ASYNC_SAMPLE_RATE) {
                    // 💥 修改：传入 contextDTO
                    result = aiAuditCoreService.doAudit(contextDTO, AuditModelLevel.CHEAP, true);
                    log.info("[异步-MQ] 抽样审核命中 | commentId={} | action={}", commentId, result.action());

                    // ===== 异步级联：高冲突场景升级强力模型 =====
                    if ("pass".equals(result.action()) && l1P1Hit) {
                        log.warn("[异步级联] 高冲突！L1命中P1但廉价模型放行，升级强力模型 | commentId={}", commentId);
                        // 💥 修改：传入 contextDTO (复用同一个上下文)
                        result = aiAuditCoreService.doAudit(contextDTO, AuditModelLevel.STRONG, false);
                        // 💥 修改日志：加上 result.getModelName()
                        log.info("[异步级联] 强力模型结果 | commentId={} | action={} | model={}", commentId, result.action(), result.modelName());
                    }
                } else {
                    // 90%未被抽样，直接确认放行，不调AI
                    log.debug("[异步-MQ] 抽样跳过，确认放行 | commentId={}", commentId);
                    commentService.lambdaUpdate()
                            .eq(Comment::getId, commentId)
                            .set(Comment::getAuditResult, "PASSED")
                            .update();
                    return;
                }
            }

            if (result == null) {
                throw new RuntimeException("审核结果异常为null");
            }

            // ===== 处理审核结果 =====
            switch (result.action()) {
                case "pass" -> {
                    commentService.lambdaUpdate()
                            .eq(Comment::getId, commentId)
                            .set(Comment::getAuditResult, "PASSED")
                            .set(Comment::getViolationTag, ViolationTag.PASS.getTag())
                            .update();
                }
                case "block" -> {
                    // 异步判违规：从VISIBLE改为SELF_ONLY (影子封禁)，如果已经是SELF_ONLY则不变
                    commentService.lambdaUpdate()
                            .eq(Comment::getId, commentId)
                            .set(Comment::getDisplayStatus, "SELF_ONLY")
                            .set(Comment::getAuditResult, "REJECTED")
                            .set(Comment::getViolationTag, ViolationTag.AI_REJECT.getTag())
                            .update();

                    // 数据回流：写入SimHash缓存，形成雪球效应
                    simHashCacheService.cache(content, ViolationTag.AI_REJECT.getTag(), "ASYNC_AI", "REJECT", result.reason());

                    if (msg.getUserId() != null) {
                        userScoreService.addScore(msg.getUserId(), ViolationTag.AI_REJECT, commentId.toString());
                    }
                }
                case "review" -> {
                    commentService.lambdaUpdate()
                            .eq(Comment::getId, commentId)
                            .set(Comment::getAuditResult, "MANUAL_REVIEW")
                            .update();
                    manualAuditTaskService.saveWithRetry(commentId, content, result.reason());
                    notifyManualReview(commentId, content, result.reason());
                }
                default -> throw new IllegalStateException("未知 action: " + result.action());
            }

            // 影子采样结果记录
            if (isShadowSource) {
                int aiLevel = convertToLevel(result);
                shadowAuditService.recordShadow(content, 999, aiLevel, !result.isPass());
            }

        } catch (Exception e) {
            log.error("[审核消费] 评论[{}]异常，转人工", commentId, e);
            commentService.lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .set(Comment::getAuditResult, "MANUAL_REVIEW")
                    .update();
            manualAuditTaskService.saveWithRetry(commentId, content, "异步消费异常：" + e.getMessage());
        }
    }

    private int convertToLevel(AuditResult result) {
        return switch (result.action()) {
            case "block" -> 3;
            case "review" -> 2;
            case "pass" -> 0;
            default -> 2;
        };
    }

    private void notifyManualReview(Long commentId, String content, String aiReason) {
        try {
            ManualAuditNotify notify = new ManualAuditNotify(commentId, content, aiReason, Instant.now().toString());
            String notifyMsg = objectMapper.writeValueAsString(notify);
            rocketMQTemplate.asyncSend(AiContentAuditConstant.MANUAL_NOTIFY_TOPIC, notifyMsg, new SendCallback() {
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