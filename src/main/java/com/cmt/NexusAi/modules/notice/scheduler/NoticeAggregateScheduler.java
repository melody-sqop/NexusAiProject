package com.cmt.NexusAi.modules.notice.scheduler;

import com.cmt.NexusAi.modules.notice.constant.NoticeConstant;
import com.cmt.NexusAi.modules.notice.enums.NoticeTargetType;
import com.cmt.NexusAi.modules.notice.enums.NoticeType;
import com.cmt.NexusAi.modules.notice.service.NoticeAggregatePersistService;
import com.cmt.NexusAi.modules.notice.util.SimpleDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 通知聚合窗口定时落盘调度器
 * 作用：唯一的清道夫。扫描过期的注册表，触发落盘。
 * 加轻量锁：防止集群部署时多台机器重复落盘同一个 Key。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeAggregateScheduler {

    private final StringRedisTemplate redisTemplate;
    private final NoticeAggregatePersistService persistService;
    private final SimpleDistributedLock distributedLock;

    @Scheduled(fixedRate = NoticeConstant.SCHEDULER_INTERVAL_MS)
    public void scanAndPersistExpiredWindows() {
        long currentTimeMs = System.currentTimeMillis();

        // 1. 扫描全局注册表中到期时间 <= 当前时间的聚合键
        Set<String> expiredKeys = redisTemplate.opsForZSet()
                .rangeByScore(NoticeConstant.AGGREGATE_WINDOWS_KEY, 0, currentTimeMs);

        if (expiredKeys == null || expiredKeys.isEmpty()) {
            return;
        }

        // 2. 遍历处理
        for (String aggregateKey : expiredKeys) {
            String lockKey = NoticeConstant.AGGREGATE_LOCK_PREFIX + aggregateKey;

            // 尝试加锁 (不需要自旋等待，拿不到说明集群中其他机器在处理，直接跳过)
            String lockValue = distributedLock.tryLock(lockKey, 10);

            if (lockValue != null) {
                try {
                    // 解析聚合键获取参数 (格式: LIKE:BLOG:100:1001)
                    String[] parts = aggregateKey.split(":");
                    NoticeType type = NoticeType.valueOf(parts[0]);
                    NoticeTargetType targetType = NoticeTargetType.valueOf(parts[1]);
                    Long targetId = Long.parseLong(parts[2]);
                    Long recipientId = Long.parseLong(parts[3]);

                    // 调用落盘服务
                    persistService.persistAggregateToDb(aggregateKey, recipientId, targetId, type, targetType);

                } catch (Exception e) {
                    log.error("定时任务聚合窗口落盘失败, aggregateKey={}", aggregateKey, e);
                } finally {
                    // 执行完毕释放锁
                    distributedLock.unlock(lockKey, lockValue);
                }
            }
        }
    }
}