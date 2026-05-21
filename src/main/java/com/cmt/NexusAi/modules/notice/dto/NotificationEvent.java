package com.cmt.NexusAi.modules.notice.dto;

import com.cmt.NexusAi.modules.notice.enums.NoticeTargetType;
import com.cmt.NexusAi.modules.notice.enums.NoticeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通知事件 DTO
 * 当业务发生（如点赞、评论）时，封装成此对象发送到 RocketMQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 接收人用户ID (谁收到通知，比如文章作者)
     */
    private Long recipientId;

    /**
     * 触发人用户ID (谁引发的通知，比如点赞人)
     */
    private Long senderId;

    /**
     * 通知类型枚举
     */
    private NoticeType type;

    /**
     * 目标类型枚举 (BLOG/COMMENT)
     */
    private NoticeTargetType targetType;

    /**
     * 目标ID (如博客ID、评论ID)
     */
    private Long targetId;

    /**
     * 评论ID (专用于评论/回复场景，用于去重和跳转定位)
     * 如果是点赞事件，此字段为空
     */
    private Long commentId;
}