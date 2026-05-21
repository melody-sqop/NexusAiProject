package com.cmt.NexusAi.modules.audit.L2b.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 统一审核路由决策服务
 * 决定评论走 同步AI / 异步MQ / 直接放行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditRouteService {

    /**
     * 路由决策结果
     */
    public enum RouteAction {
        SYNC_AI,      // 同步AI审核（高危用户，必须先审后发）
        ASYNC_MQ,     // 异步MQ审核（影子采样、P1放行验证等）
        DIRECT_PASS   // 直接放行（普通用户，发MQ做10%抽样兜底）
    }

    /**
     * 解析路由策略
     * @param forceAudit 是否命中高危行为兜底（新用户/高风险/高频留联）
     * @param isShadowSampled 是否命中影子采样
     * @return 路由动作
     */
    public RouteAction resolveRoute(boolean forceAudit, boolean isShadowSampled) {
        // 铁律1：高危用户，必须同步AI审核，阻断违规内容露出
        if (forceAudit) {
            log.debug("[路由决策] forceAudit=true → SYNC_AI");
            return RouteAction.SYNC_AI;
        }

        // 铁律2：影子采样，走异步MQ暗中验证（不影响用户体验）
        if (isShadowSampled) {
            log.debug("[路由决策] shadowSampled=true → ASYNC_MQ");
            return RouteAction.ASYNC_MQ;
        }

        // 铁律3：普通用户，直接放行，由MQ做10%抽样兜底
        log.debug("[路由决策] normal user → DIRECT_PASS");
        return RouteAction.DIRECT_PASS;
    }
}