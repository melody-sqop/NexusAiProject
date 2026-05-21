package com.cmt.NexusAi.modules.notice.mq;

import com.cmt.NexusAi.modules.notice.dto.NotificationEvent;
import com.cmt.NexusAi.modules.notice.service.NoticeTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 通知消息消费者
 * 作用：监听 notice-topic，提取 msgId 并调用事务服务处理通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = NoticeProducer.NOTICE_TOPIC, // 监听的 Topic 必须与生产者一致
        consumerGroup = "notice-consumer-group" // 消费者组名
)
public class NoticeConsumer implements RocketMQListener<Message<NotificationEvent>> {

    private final NoticeTransactionService noticeTransactionService;

    /**
     * 消费消息的方法
     * 注意：这里接收的是 Spring 的 Message 对象，因为我们需要从中提取 RocketMQ 的原生 msgId 用于去重
     */
    @Override
    public void onMessage(Message<NotificationEvent> message) {
        // 1. 获取消息体（业务数据）
        NotificationEvent event = message.getPayload();

        // 2. 获取 RocketMQ 原生的消息 ID，用于幂等去重
        // RocketMQ 将原生 ID 放在 Header 中，KEY 为 "ROCKETMQ_MSG_ID"
        String msgId = (String) message.getHeaders().get("ROCKETMQ_MSG_ID");
        if (msgId == null || msgId.isEmpty()) {
            log.error("无法获取 MQ 消息的 msgId，跳过处理: {}", event);
            return;
        }

        log.info("收到通知消息, msgId={}, type={}, recipientId={}", msgId, event.getType(), event.getRecipientId());

        try {
            // 3. 调用事务服务，执行幂等校验和落库逻辑
            boolean isSuccess = noticeTransactionService.processNotification(msgId, event);
            if (!isSuccess) {
                log.info("消息被幂等拦截，无需处理, msgId={}", msgId);
            }
        } catch (Exception e) {
            // 如果事务执行抛出异常（非幂等拦截），这里打印日志
            // RocketMQ 会根据配置进行重试
            log.error("处理通知消息异常, msgId={}", msgId, e);
            throw new RuntimeException("处理通知消息异常", e); // 抛出异常触发 MQ 重试机制
        }
    }
}