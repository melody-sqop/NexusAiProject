package com.cmt.NexusAi.modules.audit.L2a.job;

import com.cmt.NexusAi.modules.audit.L2a.mapper.ShadowLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ShadowMetricsCalculator {

    @Autowired
    private ShadowLogMapper shadowLogMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String RATE_KEY = "audit:shadow:sample:rate";

    @Scheduled(cron = "0 0 6 * * ?")
    public void calculateAndAdjust() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);

        long miss = shadowLogMapper.countMiss(start);
        long correct = shadowLogMapper.countCorrect(start);
        long total = miss + correct;

        if (total < 200) {
            log.warn("[影子调节] 近7天样本{}条，不足200，跳过", total);
            return;
        }

        double missRate = miss * 1.0 / total;
        double newRate = calcRate(missRate);

        // 幅度Clamp：单次±20%
        String oldStr = redisTemplate.opsForValue().get(RATE_KEY);
        double oldRate = (oldStr != null) ? Double.parseDouble(oldStr) : 0.05;
        double delta = newRate - oldRate;
        double clamped = Math.max(-0.20, Math.min(0.20, delta));
        double finalRate = Math.max(0.05, Math.min(0.80, oldRate + clamped));

        redisTemplate.opsForValue().set(RATE_KEY, String.valueOf(finalRate));

        log.info("[影子调节] missRate={}%, oldRate={}%, finalRate={}%, total={}",
                String.format("%.2f", missRate * 100),
                String.format("%.2f", oldRate * 100),
                String.format("%.2f", finalRate * 100),
                total);
    }

    private double calcRate(double missRate) {
        if (missRate < 0.01) return 0.05;
        if (missRate < 0.03) return 0.10;
        if (missRate < 0.05) return 0.20;
        if (missRate < 0.08) return 0.40;
        if (missRate < 0.12) return 0.60;
        return 0.80;
    }
}