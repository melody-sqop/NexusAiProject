package com.cmt.NexusAi.modules.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cmt.NexusAi.modules.notice.constant.NoticeConstant;
import com.cmt.NexusAi.modules.notice.entity.Notice;
import com.cmt.NexusAi.modules.notice.enums.NoticeTargetType;
import com.cmt.NexusAi.modules.notice.enums.NoticeType;
import com.cmt.NexusAi.modules.notice.mapper.NoticeMapper;
import com.cmt.NexusAi.modules.notice.service.NoticeAggregatePersistService;
import com.cmt.NexusAi.modules.notice.util.SimpleDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

/**
 * 通知聚合落盘服务实现 (无锁版配套)
 * 特点：只有定时任务单线程会调用此方法，因此无需考虑并发清场导致的误删问题。
 * 事务内直接清理 Redis 即可。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeAggregatePersistServiceImpl implements NoticeAggregatePersistService {

    private final StringRedisTemplate redisTemplate;
    private final NoticeMapper noticeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persistAggregateToDb(String aggregateKey, Long recipientId, Long targetId, NoticeType type, NoticeTargetType targetType) {
        String detailKey = NoticeConstant.AGGREGATE_DETAIL_PREFIX + aggregateKey;

        // 1. 获取聚合人数
        Long count = redisTemplate.opsForZSet().zCard(detailKey);
        if (count == null || count == 0) {
            cleanUpRedisKeys(aggregateKey, detailKey);
            return;
        }

        // 2. 获取最早点赞人的 senderId (取 Score 最小的)
        Set<ZSetOperations.TypedTuple<String>> earliestTuple = redisTemplate.opsForZSet().rangeWithScores(detailKey, 0, 0);
        Long firstSenderId = null;
        if (earliestTuple != null && !earliestTuple.isEmpty()) {
            String senderStr = earliestTuple.iterator().next().getValue();
            firstSenderId = senderStr != null ? Long.parseLong(senderStr) : null;
        }

        // 3. 拼装文案
        String senderName = "用户" + firstSenderId; // 模拟查库
        String content = count == 1 ?
                senderName + type.getDefaultTemplate() :
                senderName + " 等" + count + "人" + type.getDefaultTemplate();

        // 4. 构建 Notice 实体
        Notice newNotice = new Notice();
        newNotice.setRecipientId(recipientId);
        newNotice.setSenderId(firstSenderId);
        newNotice.setType(type);
        newNotice.setTargetType(targetType);
        newNotice.setTargetId(targetId);
        newNotice.setAggregateKey(aggregateKey);
        newNotice.setAggregateCount(count.intValue());
        newNotice.setContent(content);
        newNotice.setReadStatus(0);

        // 5. MySQL 落盘 (唯一索引兜底防重)
        try {
            noticeMapper.insert(newNotice);
            log.info("新增聚合通知, aggregateKey={}, count={}", aggregateKey, count);
        } catch (DuplicateKeyException e) {
            LambdaUpdateWrapper<Notice> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Notice::getRecipientId, recipientId)
                    .eq(Notice::getAggregateKey, aggregateKey)
                    .eq(Notice::getReadStatus, 0)
                    .set(Notice::getAggregateCount, count)
                    .set(Notice::getContent, content)
                    .set(Notice::getSenderId, firstSenderId);
            noticeMapper.update(null, updateWrapper);
            log.info("更新聚合通知(唯一索引拦截), aggregateKey={}, count={}", aggregateKey, count);
        }

        // 6. 事务内直接清理 Redis 双 Key (因为只有单线程定时任务执行，不怕误删)
        cleanUpRedisKeys(aggregateKey, detailKey);
    }

    private void cleanUpRedisKeys(String aggregateKey, String detailKey) {
        redisTemplate.opsForZSet().remove(NoticeConstant.AGGREGATE_WINDOWS_KEY, aggregateKey);
        redisTemplate.delete(detailKey);
    }
}