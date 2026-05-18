package com.cmt.NexusAi.modules.comment.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CommentVO {
    private Long id;
    private Long userId;
    private Long blogId;
    private Long parentId;
    private String content;

    // === 新增 ===
    private String displayStatus;
    private String auditResult;
    private String violationTag;

    private Date createTime;
    /** 前端展示文案，如"评论发布成功"、"内容已隐藏" */
    private String auditDesc;
}