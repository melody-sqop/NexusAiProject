package com.cmt.NexusAi.modules.audit.L2a.service;

import com.cmt.NexusAi.modules.audit.L2a.entity.ShadowLog;
import com.cmt.NexusAi.modules.audit.L2a.mapper.ShadowLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class ShadowAuditService {

    @Autowired
    private ShadowLogMapper shadowLogMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String RATE_KEY = "audit:shadow:sample:rate";
    private static final double DEFAULT_RATE = 0.05;

    public boolean isSampled() {
        String rateStr = redisTemplate.opsForValue().get(RATE_KEY);
        double rate = (rateStr != null) ? Double.parseDouble(rateStr) : DEFAULT_RATE;
        return ThreadLocalRandom.current().nextDouble() < rate;
    }

    public boolean isL1P1ShadowSampled() {
        return ThreadLocalRandom.current().nextInt(100) < 1;
    }

    public boolean isSimHashShadowSampled() {
        return ThreadLocalRandom.current().nextInt(100) < 1;
    }

    public void recordShadow(String content, int distance, int aiLevel, boolean isDiff) {
        try {
            ShadowLog shadowLog = new ShadowLog();
            shadowLog.setContent(content.substring(0, Math.min(200, content.length())));
            // 改：RiskLevel.SAFE(0) → "PASS"，与 violation_tag 体系对齐
            shadowLog.setCachedLevel("PASS");
            shadowLog.setAiLevel(String.valueOf(aiLevel));
            shadowLog.setDistance(distance);
            shadowLog.setIsDiff(isDiff ? 1 : 0);
            shadowLogMapper.insert(shadowLog);
            System.out.println("[影子日志] 记录成功 | aiLevel=" + aiLevel + " | isDiff=" + isDiff);
        } catch (Exception e) {
            log.error("[影子日志] 记录失败", e);
        }
    }
}