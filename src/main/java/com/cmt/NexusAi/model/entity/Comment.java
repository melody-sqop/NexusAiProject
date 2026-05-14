package com.cmt.NexusAi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("comment")
public class Comment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 博客ID
     */
    private Long blogId;

    /**
     * 父评论ID
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 审核状态：
     * 0-待审核
     * 1-通过 
     * 2-驳回
     * 3-人工复核
     * 4-审核中（MQ已消费，正在调AI）
     */
    private Integer auditStatus;

    private Date createTime;
}