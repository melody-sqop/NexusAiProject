package com.cmt.NexusAi.modules.notice.strategy;

/**
 * 用户冷热分层策略预留
 * 后期实现：识别大V(热)或僵尸用户(冷)，影响 NoticeRouter 的路由决策 (如: FANOUT 或 投降级慢队列)
 */
public interface UserTemperatureStrategy {

    /**
     * 是否为热用户（大V）
     */
    boolean isHotUser(Long userId);

    /**
     * 是否为冷用户
     */
    boolean isColdUser(Long userId);
}