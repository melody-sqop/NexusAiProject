package com.cmt.NexusAi.modules.notice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.modules.notice.dto.NotificationEvent;
import com.cmt.NexusAi.modules.notice.entity.Notice;
import com.cmt.NexusAi.modules.notice.entity.NoticeEventDedup;
import com.cmt.NexusAi.modules.notice.mapper.NoticeEventDedupMapper;
import com.cmt.NexusAi.modules.notice.mapper.NoticeMapper;
import com.cmt.NexusAi.modules.notice.service.NoticeTransactionService;
import com.cmt.NexusAi.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 通知事务服务实现类
 * 核心：利用 @Transactional 保证 dedup 插入和 notice 插入的原子性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeTransactionServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeTransactionService {

    private final NoticeEventDedupMapper noticeEventDedupMapper;
    // 假设项目中有 UserService 或 UserMapper，此处预留引用，用于查用户名拼文案
     private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启本地事务，保证原子性
    public boolean processNotification(String msgId, NotificationEvent event) {

        // --------- 第一步：幂等防重校验（利用 dedup 表唯一索引） ---------
        NoticeEventDedup dedup = new NoticeEventDedup();
        dedup.setEventId(msgId);
        dedup.setCreateTime(LocalDateTime.now());

        try {
            noticeEventDedupMapper.insert(dedup);
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突，说明是 MQ 重复投递，直接拦截
            log.info("检测到重复消费的消息, msgId={}, 已拦截", msgId);
            return false;
        }

        // --------- 第二步：查询触发人信息，拼装通知文案 ---------
        // 实际代码替换为: String senderName = userService.getUserNameById(event.getSenderId());
        String senderName = "用户" + userService.getUserNameById(event.getSenderId()); //查询用户名

        // 根据枚举自带的默认模板拼装文案，如：张三点赞了你的文章
        String content = senderName + event.getType().getDefaultTemplate();

        // --------- 第三步：构建 Notice 实体并落盘 ---------
        Notice notice = new Notice();
        notice.setRecipientId(event.getRecipientId());
        notice.setSenderId(event.getSenderId());
        notice.setType(event.getType());
        notice.setTargetType(event.getTargetType());
        notice.setTargetId(event.getTargetId());
        notice.setContent(content);
        notice.setReadStatus(0); // 默认未读
        notice.setAggregateCount(1); // 阶段2暂不处理聚合，默认1

        //TODO  预留聚合键的构建逻辑（阶段3点赞聚合会用到，评论无需聚合键）
        if (event.getCommentId() != null) {
            notice.setAggregateKey("COMMENT:" + event.getTargetId() + ":" + event.getCommentId());
        }

        baseMapper.insert(notice); // 落库

        return true;
    }
}