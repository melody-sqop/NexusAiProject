package com.cmt.NexusAi.modules.thumb.listener.consumer;

import com.cmt.NexusAi.modules.thumb.listener.ThumbEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RocketMQMessageListener(
        topic = "%DLQ%thumb-consumer-group",      // RocketMQ自动创建的死信topic
        consumerGroup = "thumb-dlq-consumer-group"
)
public class ThumbDLQConsumer implements RocketMQListener<ThumbEvent> {

    private final Counter consumerFailureCounter;

    public ThumbDLQConsumer(MeterRegistry registry) {
        this.consumerFailureCounter = Counter.builder("thumb.consumer.failure.count")
                .description("Total failed thumb messages consumed")
                .register(registry);
    }

    @Override
    public void onMessage(ThumbEvent event) {
        consumerFailureCounter.increment();
        log.error("【死信队列】点赞消息最终失败: userId={}, blogId={}, type={}",
                event.getUserId(), event.getBlogId(), event.getType());
        // TODO: 通知人工处理
    }
}