package com.cmt.NexusAi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("manual_audit_task")
public class ManualAuditTask implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联评论ID
     */
    private Long commentId;

    /**
     * 评论内容快照
     */
    private String content;

    /**
     * 审核状态：0-待处理 1-已处理
     */
    private Integer auditStatus;

    /**
     * 人工审核结果：pass/reject
     */
    private String auditResult;

    /**
     * 人工审核理由
     */
    private String auditReason;

    /**
     * AI审核理由/失败原因
     * - action=review 时：AI返回的 reason（如"涉及敏感政治人物言论，需人工确认"）
     * - 异常时：异常信息（如"Schema校验失败"、"AI服务网络异常"）
     */
    private String aiReason;

    /**
     * 审核员ID
     */
    private Long auditorId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}