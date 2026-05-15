package com.cmt.NexusAi.modules.audit.L2b.comsumer;

import com.cmt.NexusAi.modules.audit.L3.enums.ManualAuditNotify;
import com.cmt.NexusAi.modules.audit.L2b.service.AiAuditService;
import com.cmt.NexusAi.modules.audit.L3.service.ManualAuditTaskService;
import com.cmt.NexusAi.modules.audit.L2b.model.vo.AuditResult;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(String commentIdStr) {
        Long commentId = Long.parseLong(commentIdStr);
        Comment comment = commentService.getById(commentId);
        if (comment == null) return;

        // === 新增：举报复核（REJECTED）直接审，不抢锁 ===
        boolean isReportReview = (comment.getAuditStatus() == REJECTED);

        if (!isReportReview) {
            // 首次审核：PENDING → AUDITING 抢锁
            boolean locked = commentService.lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .eq(Comment::getAuditStatus, PENDING)
                    .set(Comment::getAuditStatus, AUDITING)
                    .update();
            if (!locked) {
                log.warn("[审核消费] 评论[{}]已被其他消费者处理，跳过", commentId);
                return;
            }
            comment = commentService.getById(commentId);
        }

        // 审核逻辑抽出来，首次和举报都走这里
        doAudit(comment, isReportReview);
    }

    // 抽出来的公共方法
    private void doAudit(Comment comment, boolean isReportReview) {
        Long commentId = comment.getId();
        try {
            AuditResult result = aiAuditService.auditContent(comment.getContent());
            String aiReason = result.reason();
            int finalStatus;

            switch (result.action()) {
                case "pass" -> finalStatus = PASSED;
                case "block" -> finalStatus = REJECTED;
                case "review" -> {
                    finalStatus = MANUAL_REVIEW;
                    manualAuditTaskService.saveWithRetry(commentId, comment.getContent(), aiReason);
                    notifyManualReview(commentId, comment.getContent(), aiReason);
                }
                default -> throw new IllegalStateException("未知 action");
            }

            if (!"review".equals(result.action())) {
                commentService.lambdaUpdate()
                        .eq(Comment::getId, commentId)
                        .set(Comment::getAuditStatus, finalStatus)
                        .update();
            }

            log.info("[审核消费] 评论[{}]完成，action={}，来源={}",
                    commentId, result.action(), isReportReview ? "举报" : "首次");

        } catch (Exception e) {
            log.error("[审核消费] 评论[{}]异常，转人工", commentId, e);
            commentService.lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .set(Comment::getAuditStatus, MANUAL_REVIEW)
                    .update();
            manualAuditTaskService.saveWithRetry(commentId, comment.getContent(), "异常：" + e.getMessage());
        }
    }


    private void notifyManualReview(Long commentId, String content, String aiReason) {
        try {
            ManualAuditNotify notify = new ManualAuditNotify(
                    commentId,
                    content,
                    aiReason,  // ← 传给审核系统
                    Instant.now().toString()
            );
            String notifyMsg = objectMapper.writeValueAsString(notify);
            rocketMQTemplate.asyncSend(MANUAL_NOTIFY_TOPIC, notifyMsg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("[审核消费] 人工审核通知发送成功, commentId={}", commentId);
                }

                @Override
                public void onException(Throwable e) {
                    log.error("[审核消费] 人工审核通知发送失败 commentId={}", commentId, e);
                }
            });
        } catch (Exception ex) {
            log.error("[审核消费] 构建通知消息失败 commentId={}", commentId, ex);
        }
    }
}