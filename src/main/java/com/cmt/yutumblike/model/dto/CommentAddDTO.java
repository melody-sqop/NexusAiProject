package com.cmt.yutumblike.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class CommentAddDTO {

    @NotNull(message = "博客ID不能为空")
    private Long blogId;

    // 如果是回复评论，传父评论ID，不传默认为0 0为没有顶级评论
    private Long parentId = 0L;

    @NotBlank(message = "评论内容不能为空")
    @Length(max = 1024, message = "评论内容过长")
    private String content;
}
