package com.cmt.NexusAi.modules.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.cmt.NexusAi.modules.notice.enums.NoticeTargetType;
import com.cmt.NexusAi.modules.notice.enums.NoticeType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体类，映射 notice 表
 * 用于记录站内通知的详细信息
 */
@Data
@TableName(value = "notice", autoResultMap = true) // autoResultMap 开启是为了支持枚举字段的结果映射
public class Notice {

    /**
     * 通知ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收人用户ID (谁收到了这个通知)
     */
    private Long recipientId;

    /**
     * 触发人用户ID (谁引发了这个通知，如点赞人、评论人)
     * 注意：可能为空，预留系统通知的场景
     */
    private Long senderId;

    /**
     * 通知类型
     * 使用 @com.baomidou.mybatisplus.annotation.EnumValue 注解无效时，
     * 依赖全局配置 mybatis-plus.configuration.default-enum-type-handler=org.apache.ibatis.type.EnumTypeHandler
     * 或者在此处使用 @EnumValue 注解（推荐在枚举类字段上加）
     */
    private NoticeType type;

    /**
     * 目标类型 (BLOG/COMMENT)
     */
    private NoticeTargetType targetType;

    /**
     * 目标ID (如博客ID、评论ID)，与 targetType 配合定位具体资源
     */
    private Long targetId;

    /**
     * 聚合键 (用于点赞合并，如 LIKE:123:456 表示用户123对文章456的点赞聚合)
     */
    private String aggregateKey;

    /**
     * 聚合数量，默认1。若大于1，表示这是一条聚合通知（如"张三、李四等5人点赞了"）
     */
    private Integer aggregateCount;

    /**
     * 通知文案，直接存储前端展示的文本，避免后续关联查询
     */
    private String content;

    /**
     * 已读状态：0未读，1已读
     */
    private Integer readStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}