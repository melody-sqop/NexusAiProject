package com.cmt.NexusAi.modules.audit.L2b.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditMessageDTO {
    private Long commentId;
    private Long userId;
    private String source;
    private String reason;
    private boolean needFullAudit; // true=高危/影子100%审, false=普通10%抽样
    private boolean l1P1Hit;       // true=L1命中过P1敏感词, 用于异步级联判断

    // 1.3 新增：上下文快照
    private String scene;          // 业务场景
    private String parentTitle;    // 父级标题
}