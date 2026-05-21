package com.cmt.NexusAi.modules.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知事件幂等去重表 实体类
 * 作用：纯防 MQ 重复消费的"签到表"，只认 MQ 的 msgId
 */
@Data
@TableName("notice_event_dedup")
public class NoticeEventDedup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * MQ消息的唯一ID (RocketMQ 的 msgId)
     */
    private String eventId;

    private LocalDateTime createTime;
}