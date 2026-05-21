package com.cmt.NexusAi.modules.audit.L2b.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditContextDTO {
    private String content;      // 评论原文
    private String scene;        // 业务场景 (如 BLOG_COMMENT, LIVE_CHAT)
    private String parentTitle;  // 父级标题快照
}