package com.cmt.NexusAi.modules.comment.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("comment")
public class Comment {
    private Long id;
    private Long blogId;
    private Long userId;
    private String content;
    private Long parentId;
    private String displayStatus;
    private String auditResult;
    private String violationTag;
    private Date createTime;
    private Date updateTime;

    // 1.3 新增：父级标题快照
    private String parentTitle;
}