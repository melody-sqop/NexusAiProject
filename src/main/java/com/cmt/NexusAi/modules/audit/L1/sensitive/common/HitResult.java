package com.cmt.NexusAi.modules.audit.L1.sensitive.common;

import lombok.Data;

/**
 * 单次命中结果
 */
@Data
public class HitResult {

    // 在原文中的开始位置
    private int start;

    // 在原文中的结束位置
    private int end;

    // 命中的敏感词（原始词，如"法轮功"）
    private String matchedWord;

    // 风险等级：P0、P1、P2
    private String riskLevel;

    // 分类：涉政、色情、暴恐等
    private String category;
}