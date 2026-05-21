package com.cmt.NexusAi.modules.comment.model.dto;

import lombok.Data;

@Data
public class CommentAddDTO {
    private Long blogId;
    private String content;
    private Long parentId;

    // 1.3 新增：业务场景标识
    private String scene; // 例如: BLOG_COMMENT
}