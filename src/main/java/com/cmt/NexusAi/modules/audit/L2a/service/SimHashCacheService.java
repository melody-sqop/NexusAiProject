package com.cmt.NexusAi.modules.audit.L2a.service;

import com.cmt.NexusAi.modules.audit.L2b.model.DTO.AuditResultDTO;
import com.cmt.NexusAi.modules.audit.L2b.enums.RiskLevel;
import com.cmt.NexusAi.modules.audit.L2a.util.SimHashUtil;
import com.cmt.NexusAi.modules.audit.L2b.model.vo.AuditResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SimHashCacheService {

    @Autowired
    private StringRedisTemplate redis;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 写入缓存。只存违规内容（P1/P2/P3），P4正常和P0涉政不存。
     */
    public void cache(String content, AuditResultDTO result) {
        if (result.getLevel() == RiskLevel.CRITICAL || result.getLevel() == RiskLevel.SAFE) {
            return;
        }

        long hash = SimHashUtil.compute(content);
        int[] segs = SimHashUtil.split4(hash);

        String mainKey = "simhash:feature:" + hash;
        try {
            redis.opsForValue().set(mainKey, mapper.writeValueAsString(result), 24, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("SimHash缓存序列化失败", e);
            return;
        }

        for (int i = 0; i < 4; i++) {
            String bucketKey = "simhash:bucket:" + i + ":" + segs[i];
            redis.opsForSet().add(bucketKey, String.valueOf(hash));
        }
    }

    /**
     * 查询缓存。threshold=2 表示最多允许2位不同。
     */
    public AuditResult query(String content, int threshold) {
        long hash = SimHashUtil.compute(content);
        int[] segs = SimHashUtil.split4(hash);

        Set<String> candidates = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            String bucketKey = "simhash:bucket:" + i + ":" + segs[i];
            Set<String> list = redis.opsForSet().members(bucketKey);
            if (list != null) candidates.addAll(list);
        }

        int minDist = Integer.MAX_VALUE;
        String bestHash = null;

        for (String cand : candidates) {
            long c = Long.parseLong(cand);
            int dist = SimHashUtil.hammingDistance(hash, c);
            if (dist < minDist) {
                minDist = dist;
                bestHash = cand;
            }
        }

        if (minDist > threshold || bestHash == null) {
            return null;
        }

        String mainKey = "simhash:feature:" + bestHash;
        String json = redis.opsForValue().get(mainKey);
        if (json == null || json.isEmpty()) return null;

        try {
            return mapper.readValue(json, AuditResult.class);
        } catch (JsonProcessingException e) {
            log.error("SimHash缓存反序列化失败", e);
            return null;
        }
    }
}