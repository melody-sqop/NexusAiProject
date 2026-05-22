package com.cmt.NexusAi.modules.notice.service;


import com.cmt.NexusAi.modules.notice.enums.NoticeTargetType;
import com.cmt.NexusAi.modules.notice.enums.NoticeType;

/**
 * 通知聚合落盘服务接口
 * 作用：定义从 Redis 聚合窗口将数据持久化到 MySQL 的行为。
 */
public interface NoticeAggregatePersistService {
    /**
     * 将指定聚合键的数据落盘到 MySQL
     * @param aggregateKey 聚合键
     * @param recipientId 接收人ID
     * @param targetId 目标ID
     * @param type 通知类型
     * @param targetType 目标类型
     */
    void persistAggregateToDb(String aggregateKey, Long recipientId, Long targetId, NoticeType type, NoticeTargetType targetType);

}