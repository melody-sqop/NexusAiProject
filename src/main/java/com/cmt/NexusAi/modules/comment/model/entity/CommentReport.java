package com.cmt.NexusAi.modules.comment.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("comment_report")
public class CommentReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long commentId;

    private Long reporterId;

    private Integer reportReason;

    private String reportDesc;

    private Integer status;  // 0-待处理 1-确认违规 2-误报

    private Date createdAt;

    private Date updatedAt;
}