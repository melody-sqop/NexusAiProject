package com.cmt.NexusAi.ai.consumer;


import com.cmt.NexusAi.ai.constant.AiContentAuditConstant;
import com.cmt.NexusAi.ai.model.ManualAuditNotify;
import com.cmt.NexusAi.ai.service.AiAuditService;
import com.cmt.NexusAi.model.entity.Comment;
import com.cmt.NexusAi.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.cmt.NexusAi.ai.constant.AiContentAuditConstant.AUDIT_TOPIC;
import static com.cmt.NexusAi.ai.constant.AiContentAuditConstant.MANUAL_NOTIFY_TOPIC;

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

        if (comment == null || comment.getAuditStatus() != AiContentAuditConstant.PENDING) {
            return;
        }

        boolean isPass;
        try {
            // 调用审核
            isPass = aiAuditService.auditContent(comment.getContent());

            // 更新状态：通过或驳回
            Comment update = new Comment();
            update.setId(commentId);
            update.setAuditStatus(isPass ? AiContentAuditConstant.PASSED : AiContentAuditConstant.REJECTED);
            commentService.updateById(update);

            log.info("评论[{}]审核完成：{}", commentId, isPass ? "通过" : "驳回");

        } catch (Exception e) {
            // 走到这里说明：重试3次后还是网络错误，或者出现了意料之外的异常
            Comment update = new Comment();
            update.setId(commentId);
            update.setAuditStatus(AiContentAuditConstant.MANUAL_REVIEW);
            commentService.updateById(update);

            notifyManualReview(commentId, comment.getContent());
            log.error("评论[{}] AI审核最终失败，转人工", commentId, e);
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