package com.cmt.NexusAi.modules.notice.service.impl;

import com.cmt.NexusAi.modules.notice.constant.NoticeConstant;
import com.cmt.NexusAi.modules.notice.dto.NotificationEvent;
import com.cmt.NexusAi.modules.notice.service.NoticeAggregatePersistService;
import com.cmt.NexusAi.modules.notice.service.RedisAggregateWindowService;
import com.cmt.NexusAi.modules.notice.util.SimpleDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 聚合窗口服务 (无锁最终一致版)
 * 核心思想：应用层只管无脑 ZADD，把判断和清场权完全下放给单线程的定时任务。
 * 优势：纯内存操作，无锁无阻塞，性能拉满，撑住每秒数万点赞毫无压力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisAggregateWindowServiceImpl implements RedisAggregateWindowService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addToWindow(NotificationEvent event) {
        String aggregateKey = buildAggregateKey(event);
        String detailKey = NoticeConstant.AGGREGATE_DETAIL_PREFIX + aggregateKey;
        long currentTimeMs = System.currentTimeMillis();
        String senderIdStr = String.valueOf(event.getSenderId());

        // 1. 无脑写入明细 ZSet (不管过不过期，超不超容量，先进堆再说)
        redisTemplate.opsForZSet().add(detailKey, senderIdStr, currentTimeMs);
        // 兜底 TTL 2小时
        redisTemplate.expire(detailKey, NoticeConstant.DETAIL_KEY_TTL_SECONDS, TimeUnit.SECONDS);

        // 2. 计算到期时间 (核心：依靠 Math.min 卡死5分钟上限)
        // 即使窗口已过期，当前时间+60s 依然会被 Math.min 卡在死线上，保证定时任务能扫出来
        long expireTimeMs = calculateExpireTime(detailKey, currentTimeMs);

        // 3. 无脑写入全局注册表 ZSet
        redisTemplate.opsForZSet().add(NoticeConstant.AGGREGATE_WINDOWS_KEY, aggregateKey, expireTimeMs);
    }

    /**
     * 计算窗口到期时间
     * 逻辑：取 min(窗口首次开启时间 + 5分钟, 当前时间 + 60秒)
     */
    private long calculateExpireTime(String detailKey, long currentTimeMs) {
        Set<ZSetOperations.TypedTuple<String>> earliestTuple = redisTemplate.opsForZSet()
                .rangeWithScores(detailKey, 0, 0);
        long windowStartTimeMs = currentTimeMs;
        if (earliestTuple != null && !earliestTuple.isEmpty()) {
            Double score = earliestTuple.iterator().next().getScore();
            if (score != null) {
                windowStartTimeMs = score.longValue();
            }
        }
        long maxExpireTimeMs = windowStartTimeMs + NoticeConstant.WINDOW_MAX_LIFESPAN_SECONDS * 1000;
        long slideExpireTimeMs = currentTimeMs + NoticeConstant.WINDOW_SLIDE_SECONDS * 1000;
        return Math.min(maxExpireTimeMs, slideExpireTimeMs);
    }

    private String buildAggregateKey(NotificationEvent event) {
        return event.getType().name() + ":" + event.getTargetType().name() + ":" + event.getTargetId() + ":" + event.getRecipientId();
    }
}