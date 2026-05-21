package com.cmt.NexusAi.modules.audit.L2b.enums;

/**
 * AI审核模型级别定义
 * 用于控制模型分级路由，实现降本增效
 */
public enum AuditModelLevel {
    /**
     * 中等模型（如 Qwen-Plus / GPT-4o-mini）
     * 用于：同步链路高危审核。兼顾准确率和RT(200-400ms)
     */
    MEDIUM,

    /**
     * 廉价快模型（如 Qwen-Turbo / GPT-3.5）
     * 用于：异步链路10%抽样初审。极致省钱，RT极快
     */
    CHEAP,

    /**
     * 强力模型（如 GPT-4o / Qwen-Max）
     * 用于：异步链路高冲突场景复核（L1命中+廉价模型放行）。最强推理能力
     */
    STRONG
}