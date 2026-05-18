package com.cmt.NexusAi.modules.comment.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long blogId;
    private Long parentId;
    private String content;

    // === 新增：替代原 audit_status（TINYINT） ===
    /** 前端展示控制：VISIBLE/HIDDEN/SELF_ONLY */
    private String displayStatus;
    /** 后台审核结论：PENDING/PASSED/REJECTED/MANUAL_REVIEW */
    private String auditResult;
    /** 违规标签：PASS/P0_BLACKLIST/P1_CONTACT/AI_REJECT/REPORT_HIT */
    private String violationTag;

    private Date createTime;
}