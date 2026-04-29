package com.cmt.yutumblike.ai.consumer;


import com.cmt.yutumblike.ai.constant.AiContentAuditConstant;
import com.cmt.yutumblike.ai.model.ManualAuditNotify;
import com.cmt.yutumblike.ai.service.AiAuditService;
import com.cmt.yutumblike.model.entity.Comment;
import com.cmt.yutumblike.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.cmt.yutumblike.ai.constant.AiContentAuditConstant.AUDIT_TOPIC;
import static com.cmt.yutumblike.ai.constant.AiContentAuditConstant.MANUAL_NOTIFY_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiContentAuditConsumer {

    private final CommentService commentService;

    private final PulsarTemplate<String> pulsarTemplate;

    private final AiAuditService aiAuditService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 消费者调用ai审核评论是否 通过
     *
     * @param commentIdStr 评论id
     *                     目前低并发场景不做batch处理 一条一条处理
     */
    @PulsarListener(
            //TODO 后期可以增加死信队列完善健壮性
//            deadLetterPolicy = @DeadLetterPolicy(
//                    maxRedeliverCount = 3,
//                    deadLetterTopic = "comment-audit-dlq"
//            ),
            topics = AUDIT_TOPIC,
            subscriptionName = "comment-audit-sub",
            subscriptionType = SubscriptionType.Shared
    )
    public void handleCommentAudit(String commentIdStr) {
        Long commentId = Long.parseLong(commentIdStr);
        Comment comment = commentService.getById(commentId);

        if (comment == null || comment.getAuditStatus() != 0) {
            return; // 已处理或不存在，直接ACK
        }

        try {
            // ✅ 调用独立Service，@Retryable 生效
            boolean isPass = aiAuditService.auditContent(comment.getContent());

            Comment update = new Comment();
            update.setId(commentId);
            update.setAuditStatus(isPass ? AiContentAuditConstant.PASSED : AiContentAuditConstant.REJECTED);
            commentService.updateById(update);
            log.info("评论[{}]审核完成：{}", commentId, update.getAuditStatus());

        } catch (Exception e) {
            // ⚠️ 注意：这里已经是 @Retryable 重试耗尽后的最终异常
            Comment update = new Comment();
            update.setId(commentId);
            update.setAuditStatus(AiContentAuditConstant.MANUAL_REVIEW);
            commentService.updateById(update);

            // 📤 发送人工审核通知（异步，不阻塞主流程）
            notifyManualReview(commentId, comment.getContent());

            log.error("评论[{}] AI重试耗尽，已转人工审核", commentId, e);
            // 🔑 关键：不要往外抛异常！否则 Pulsar 会 NACK 并无限重试本条消息
        }
    }

    private void notifyManualReview(Long commentId, String content) {
        try {
            // 2. 序列化发送
            ManualAuditNotify notify = new ManualAuditNotify(
                    commentId,
                    content,
                    Instant.now().toString()
            );
            String notifyMsg = objectMapper.writeValueAsString(notify);
            pulsarTemplate.sendAsync(MANUAL_NOTIFY_TOPIC, notifyMsg);
        } catch (Exception ex) {
            log.error("发送人工审核通知失败 commentId={}", commentId, ex);
        }
    }


}