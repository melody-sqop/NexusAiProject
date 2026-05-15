package com.cmt.NexusAi.modules.audit.L2b.model.vo;

public record AuditResult(

        /**
         * 表示合规，false 表示违规
         */
        Boolean isPass,

        /**
         * 主要违规类型（如 "人身攻击"），合规时填 null
         */
        String violationType,
        /**
         * 判定依据（简明客观，≤30字）
         */
        String reason,

        /**
         * 建议（如 "pass" | "block" | "review"）
         */
        String action
) {}