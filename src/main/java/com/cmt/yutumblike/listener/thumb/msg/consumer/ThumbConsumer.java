package com.cmt.yutumblike.listener.thumb.msg.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmt.yutumblike.listener.thumb.msg.ThumbEvent;
import com.cmt.yutumblike.mapper.BlogMapper;
import com.cmt.yutumblike.model.entity.Thumb;
import com.cmt.yutumblike.service.ThumbService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.apache.pulsar.shade.org.apache.commons.lang3.tuple.Pair;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ThumbConsumer {

    private final BlogMapper blogMapper;
    private final ThumbService thumbService;
    private final Counter consumerSuccessCounter;
    private final Counter consumerFailureCounter;

    // 完全手写构造函数，注入所有依赖
    public ThumbConsumer(BlogMapper blogMapper,
                         ThumbService thumbService,
                         MeterRegistry registry) {
        this.blogMapper = blogMapper;
        this.thumbService = thumbService;
        this.consumerSuccessCounter = Counter.builder("thumb.consumer.success.count")
                .description("Total successful thumb messages consumed (消费层)")
                .register(registry);
        this.consumerFailureCounter = Counter.builder("thumb.consumer.failure.count")
                .description("Total failed thumb messages consumed (消费层)")
                .register(registry);
    }




    // 死信队列
    @PulsarListener(topics = "thumb-dlq-topic")
    public void consumerDlq(Message<ThumbEvent> message) {
        consumerFailureCounter.increment();
        MessageId messageId = message.getMessageId();
        log.info("dlq message = {}", messageId);
        log.info("消息 {} 已入库", messageId);
        log.info("已通知相关人员 {} 处理消息 {}", "坤哥", messageId);
    }




    // 批量处理配置
    @PulsarListener(
            subscriptionName = "thumb-subscription",
            topics = "thumb-topic",
            schemaType = SchemaType.JSON,
            batch = true, // 开启批量处理
//            consumerCustomizer = "thumbConsumerConfig",
            // 引用 NACK 重试策略
            negativeAckRedeliveryBackoff = "negativeAckRedeliveryBackoff",
            // 引用 ACK 超时重试策略
            ackTimeoutRedeliveryBackoff = "ackTimeoutRedeliveryBackoff",
            // 添加死信队列
            deadLetterPolicy = "deadLetterPolicy",
            // Shared 模式 死信队列必须用这个
            subscriptionType = SubscriptionType.Shared
    )
    @Transactional(rollbackFor = Exception.class)
    public void processBatch(List<Message<ThumbEvent>> messages) {
        log.info("ThumbConsumer processBatch: {}", messages.size());

        // 统计每篇博客的点赞数变化量，用于批量更新博客的点赞数
        Map<Long, Long> countMap = new ConcurrentHashMap<>();

        // 收集所有要插入数据库的点赞记录，用于批量插入
        List<Thumb> thumbs = new ArrayList<>();

        // 构建删除条件，用于批量删除取消点赞的记录。
        LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();

        // 判断这批数据最终需要不需要进行删除点赞记录
        AtomicReference<Boolean> needRemove = new AtomicReference<>(false);

        // 提取事件并过滤无效消息
        List<ThumbEvent> events = messages.stream()
                .map(Message::getValue)
                .filter(Objects::nonNull)
                .toList();


        // 按(userId, blogId)分组，并获取每个分组的最新事件
        Map<Pair<Long, Long>, ThumbEvent> latestEvents = events.stream()
                .collect(Collectors.groupingBy(
                        e -> Pair.of(e.getUserId(), e.getBlogId()),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    // 按时间升序排序，取最后一个作为最新事件
                                    list.sort(Comparator.comparing(ThumbEvent::getEventTime));
                                    if (list.size() % 2 == 0) {
                                        return null;
                                    }
                                    return list.get(list.size() - 1);
                                }
                        )
                ));

        latestEvents.forEach((userBlogPair, event) -> {
            if (event == null) {
                return;
            }
            ThumbEvent.EventType finalAction = event.getType();

            if (finalAction == ThumbEvent.EventType.INCR) {
                countMap.merge(event.getBlogId(), 1L, Long::sum);
                Thumb thumb = new Thumb();
                thumb.setBlogId(event.getBlogId());
                thumb.setUserId(event.getUserId());
                thumbs.add(thumb);
                consumerSuccessCounter.increment();
            } else {
                needRemove.set(true);
                wrapper.or().eq(Thumb::getUserId, event.getUserId()).eq(Thumb::getBlogId, event.getBlogId());
                countMap.merge(event.getBlogId(), -1L, Long::sum);
                consumerSuccessCounter.increment();
            }
        });

        // 批量更新数据库
        if (needRemove.get()) {
            thumbService.remove(wrapper);
        }
        // 批量更新博客点赞数
        batchUpdateBlogs(countMap);

        batchInsertThumbs(thumbs);
    }


    public void batchUpdateBlogs(Map<Long, Long> countMap) {
        if (!countMap.isEmpty()) {
            blogMapper.batchUpdateThumbCount(countMap);
        }
    }

    public void batchInsertThumbs(List<Thumb> thumbs) {
        if (!thumbs.isEmpty()) {
            // 分批次插入
            thumbService.saveBatch(thumbs, 500);
        }
    }
}
