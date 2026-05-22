package com.cmt.NexusAi.modules.notice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.modules.notice.dto.NotificationEvent;
import com.cmt.NexusAi.modules.notice.entity.Notice;
import com.cmt.NexusAi.modules.notice.entity.NoticeEventDedup;
import com.cmt.NexusAi.modules.notice.enums.NoticeRoute;
import com.cmt.NexusAi.modules.notice.mapper.NoticeEventDedupMapper;
import com.cmt.NexusAi.modules.notice.mapper.NoticeMapper;
import com.cmt.NexusAi.modules.notice.router.NoticeRouter;
import com.cmt.NexusAi.modules.notice.service.NoticeTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 通知事务性落库服务实现
 * 作用：消费 MQ 消息后的入口处理类。
 * 核心职责：
 * 1. 保证幂等性（通过 notice_event_dedup 唯一索引防重）。
 * 2. 路由分流：根据 NoticeRouter 将消息分发到 MySQL 直达 或 Redis 聚合。
 * 3. 保证事务与 Redis 操作的隔离：Redis 写操作必须在数据库事务 afterCommit 后执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeTransactionServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeTransactionService {

    private final NoticeEventDedupMapper dedupMapper;
    private final NoticeRouter noticeRouter;
    private final RedisAggregateWindowServiceImpl redisAggregateWindowService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processNotification(String msgId, NotificationEvent event) {
        // 1. 幂等防重校验 (纯 MySQL 唯一索引方案)
        NoticeEventDedup dedup = new NoticeEventDedup();
        dedup.setEventId(msgId);
        dedup.setCreateTime(java.time.LocalDateTime.now());
        try {
            dedupMapper.insert(dedup);
        } catch (DuplicateKeyException e) {
            log.info("检测到重复消费的消息, msgId={}, 已拦截", msgId);
            return false;
        }

        // 2. 路由分流
        NoticeRoute route = noticeRouter.route(event);

        if (route == NoticeRoute.HIGH_DIRECT) {
            // 3.1 直达链路 (评论/回复)：在当前事务内直接入库
            processDirectly(event);
        } else if (route == NoticeRoute.AGGREGATE) {
            // 3.2 聚合链路 (点赞)：事务提交成功后，推入 Redis 滑动窗口
            registerAfterCommitForAggregation(event);
        }

        return true;
    }

    /**
     * 直达处理逻辑
     */
    private void processDirectly(NotificationEvent event) {
        String senderName = "用户" + event.getSenderId(); // 模拟查询用户服务
        String content = senderName + event.getType().getDefaultTemplate();

        Notice notice = new Notice();
        notice.setRecipientId(event.getRecipientId());
        notice.setSenderId(event.getSenderId());
        notice.setType(event.getType());
        notice.setTargetType(event.getTargetType());
        notice.setTargetId(event.getTargetId());
        notice.setContent(content);
        notice.setReadStatus(0);
        notice.setAggregateCount(1);

        // 评论类构建聚合键
        if (event.getCommentId() != null) {
            notice.setAggregateKey("COMMENT:" + event.getTargetId() + ":" + event.getCommentId());
        }

        baseMapper.insert(notice);
        // TODO: 触发 SSE 推送 refresh 信号给 recipientId (阶段4实现)
    }

    /**
     * 注册事务提交后的回调，用于将事件推入 Redis
     * 为什么不在业务方法里直接调 Redis？因为如果数据库事务提交失败，Redis 里的数据就成了脏数据。
     */
    private void registerAfterCommitForAggregation(NotificationEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    redisAggregateWindowService.addToWindow(event);
                } catch (Exception e) {
                    log.error("推入Redis聚合窗口失败, event={}", event, e);
                    // 此处异常不会导致数据库事务回滚(已提交)。若需极高可靠性，可在此发一条补救MQ消息。
                }
            }
        });
    }
}