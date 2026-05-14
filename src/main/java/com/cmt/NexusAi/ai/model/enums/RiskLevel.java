package com.cmt.NexusAi.ai.model.enums;

import com.cmt.NexusAi.ai.common.HitResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 风险分级决策
 *
 * 业务逻辑：
 * 1. 用户发内容 → DFA匹配返回 List<HitResult>
 * 2. 每个 HitResult 里已经有 riskLevel（P0/P1/P2）
 * 3. 命中多个词 → 取风险等级最高的
 * 4. 如果是标题/昵称 → 升一级处理
 * 5. 输出 Result，告诉系统该拦截/送AI/放行
 */
@Slf4j
public class RiskLevel {

    // ========== 5个数字等级 ==========
    public static final int SAFE = 0;       // 安全，放行
    public static final int LOW = 1;        // 低风险，提示修改
    public static final int MEDIUM = 2;     // 中风险，删除+限流
    public static final int HIGH = 3;       // 高风险，删除+禁言
    public static final int CRITICAL = 4;   // 极高风险，拦截+封号

    // ========== 数据库P0/P1/P2 转数字 ==========
    public static int fromDb(String dbCode) {
        if (dbCode == null || dbCode.isBlank()) {
            return MEDIUM;  // 数据异常，保守兜底
        }
        return switch (dbCode.toUpperCase()) {
            case "P0" -> CRITICAL;   // 涉政/暴恐/毒品
            case "P1" -> HIGH;       // 色情/赌博/诈骗
            case "P2" -> MEDIUM;     // 广告/导流
            case "P3" -> LOW;        // 辱骂
            case "P4" -> SAFE;       // 白名单/仅记录
            default -> MEDIUM;
        };
    }

    // ========== 数字转业务动作 ==========
    public static String toAction(int level, boolean isTitle) {
        // 标题升一级（封顶CRITICAL）
        if (isTitle && level < CRITICAL) {
            level++;
        }
        return switch (level) {
            case SAFE -> "放行";
            case LOW -> "提示修改";
            case MEDIUM -> "删除+限流";
            case HIGH -> "删除+禁言";
            case CRITICAL -> "拦截+封号";
            default -> "送AI";
        };
    }

    // ========== 核心决策入口 ==========
    /**
     * @param hits     DFA匹配结果（你现有的 HitResult 列表）
     * @param isTitle  是否标题/昵称
     */
    public static Result decide(List<HitResult> hits, boolean isTitle) {
        // 没命中 → 安全放行
        if (hits == null || hits.isEmpty()) {
            return new Result(SAFE, "放行", List.of());
        }

        // 取最高风险等级（比如同时命中P0和P2，取P0）
        int maxLevel = hits.stream()
                .mapToInt(h -> fromDb(h.getRiskLevel()))
                .max()
                .orElse(SAFE);

        int finalLevel = maxLevel;  // ← 定义 finalLevel

        if (isTitle && finalLevel < CRITICAL) {
            finalLevel++;
            log.info("[SENSITIVE-DECIDE] 标题加权升级 | original={} | upgraded={} | hits={}",
                    maxLevel, finalLevel, hits.size());
        }

        log.info("[SENSITIVE-DECIDE] 决策结果 | isTitle={} | finalLevel={} | hitWords={}",
                isTitle, finalLevel,
                hits.stream().map(HitResult::getMatchedWord).collect(Collectors.toList()));


        // 全是SAFE级（白名单词）→ 直接放行
        if (maxLevel == SAFE) {
            return new Result(SAFE, "放行", hits);
        }

        String action = toAction(maxLevel, isTitle);
        return new Result(maxLevel, action, hits);
    }

    // ========== 决策结果 ==========
    @Data
    public static class Result {
        private final int level;              // 最终等级 0~4
        private final String action;            // 业务动作
        private final List<HitResult> hits;     // 命中详情（直接用你的HitResult）
        private final long time = System.currentTimeMillis();

        // 是否立即拦截（只有CRITICAL直接拦）
        public boolean interceptNow() {
            return level == CRITICAL;
        }

        // 是否送AI（HIGH必送；MEDIUM且多命中也送）
        public boolean needAi() {
            return level == HIGH
                    || (level == MEDIUM && hits.size() >= 2);
        }
    }
}