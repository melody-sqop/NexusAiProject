package com.cmt.NexusAi.listener.thumb.msg.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmt.NexusAi.listener.thumb.msg.ThumbEvent;
import com.cmt.NexusAi.mapper.BlogMapper;
import com.cmt.NexusAi.model.entity.Thumb;
import com.cmt.NexusAi.service.ThumbService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RocketMQMessageListener(
        topic = "thumb-topic",
        consumerGroup = "thumb-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class ThumbConsumer implements RocketMQListener<ThumbEvent> {

    private final BlogMapper blogMapper;
    private final ThumbService thumbService;
    private final Counter consumerSuccessCounter;
    private final Counter consumerFailureCounter;

    public ThumbConsumer(BlogMapper blogMapper,
                         ThumbService thumbService,
                         MeterRegistry registry) {
        this.blogMapper = blogMapper;
        this.thumbService = thumbService;
        this.consumerSuccessCounter = Counter.builder("thumb.consumer.success.count")
                .description("Total successful thumb messages consumed")
                .register(registry);
        this.consumerFailureCounter = Counter.builder("thumb.consumer.failure.count")
                .description("Total failed thumb messages consumed")
                .register(registry);
    }

    @Override
    public void onMessage(ThumbEvent event) {
        log.info("处理点赞事件: userId={}, blogId={}, type={}",
                event.getUserId(), event.getBlogId(), event.getType());

        try {
            // 用你原来的 batchUpdateThumbCount 方法
            Map<Long, Long> countMap = new HashMap<>();

            if (event.getType() == ThumbEvent.EventType.INCR) {
                countMap.put(event.getBlogId(), 1L);

                Thumb thumb = new Thumb();
                thumb.setBlogId(event.getBlogId());
                thumb.setUserId(event.getUserId());
                thumbService.save(thumb);
            } else {
                countMap.put(event.getBlogId(), -1L);

                LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Thumb::getUserId, event.getUserId())
                        .eq(Thumb::getBlogId, event.getBlogId());
                thumbService.remove(wrapper);
            }

            // 批量更新点赞数
            blogMapper.batchUpdateThumbCount(countMap);

            consumerSuccessCounter.increment();

        } catch (Exception e) {
            consumerFailureCounter.increment();
            log.error("处理点赞事件失败", e);
            throw e;  // 抛异常触发 RocketMQ 重试
        }
    }
}