package com.cmt.NexusAi.modules.notice.mq;

import com.cmt.NexusAi.modules.notice.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 通知消息生产者
 * 作用：将 NotificationEvent 发送到 RocketMQ 的指定 Topic
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 通知业务专用的 Topic 名称
     */
    public static final String NOTICE_TOPIC = "notice-topic";

    /**
     * 发送通知事件（异步发送，不阻塞主业务）
     * @param event 通知事件对象
     */
    public void sendNotification(NotificationEvent event) {
        try {
            rocketMQTemplate.asyncSend(NOTICE_TOPIC, MessageBuilder.withPayload(event).build(), new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                    log.info("通知事件发送成功, type={}, recipientId={}", event.getType(), event.getRecipientId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("通知事件发送失败, type={}, recipientId={}", event.getType(), event.getRecipientId(), e);
                    // 根据业务诉求，这里可以加上降级逻辑（如存库重试）或报警
                }
            });
        } catch (Exception e) {
            log.error("通知事件发送异常", e);
        }
    }
}