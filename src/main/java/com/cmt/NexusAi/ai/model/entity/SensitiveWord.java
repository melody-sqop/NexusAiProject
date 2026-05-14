package com.cmt.NexusAi.ai.model.entity;

import lombok.Data;

/**
 * 敏感词实体
 * 对应数据库表 sensitive_word 的一条记录
 */
@Data
public class SensitiveWord {

    // 敏感词ID，数据库自增
    private Long id;

    // 敏感词内容，如 "法轮功"
    private String word;

    // 风险等级：P0=直接拦截，P1=人工审核，P2=AI审核
    private String riskLevel;

    // 分类：涉政、色情、暴恐、广告等
    private String category;
}