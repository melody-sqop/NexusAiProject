package com.cmt.NexusAi.modules.audit.L2a.service;

import com.cmt.NexusAi.modules.audit.L2a.entity.SimHashResult;
import com.cmt.NexusAi.modules.audit.L2a.util.SimHashUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class SimHashCacheService {

    private static final String PREFIX = "simhash:";
    private static final long TTL_HOURS = 24; // 24小时自愈
    private static final int THRESHOLD = 3;
    private static final int SEGMENTS = 4;
    private static final int BITS_PER_SEGMENT = 16;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    public SimHashResult query(String text) {
        return findSimilar(text);
    }

    /**
     * 写入 SimHash 缓存
     * 仅缓存明确REJECT，P0涉政/灰色地带/REVIEW 均跳过
     */
    public void cache(String text, String violationTag, String auditSource,
                      String auditResult, String auditComment) {
        if ("P0_BLACKLIST".equals(violationTag)) {
            log.warn("[SimHash] P0涉政跳过 | text={}", truncate(text, 30));
            return;
        }

        if (!"REJECT".equals(auditResult)) {
            log.debug("[SimHash] 非REJECT跳过 | result={} | text={}", auditResult, truncate(text, 30));
            return;
        }

        try {
            long simHash = SimHashUtil.computeSimHash(text);

            List<String> segmentKeys = new ArrayList<>(SEGMENTS);
            for (int i = 0; i < SEGMENTS; i++) {
                segmentKeys.add(String.valueOf(extractSegment(simHash, i)));
            }

            SimHashResult result = SimHashResult.builder()
                    .simHash(simHash)
                    .segmentKeys(segmentKeys)
                    .violationTag(violationTag)
                    .auditSource(auditSource)
                    .auditResult(auditResult)
                    .auditComment(auditComment)
                    .sampleText(truncate(text, 100))
                    .createTime(LocalDateTime.now().format(TIME_FMT))
                    .build();

            // 写入分段桶
            for (int i = 0; i < SEGMENTS; i++) {
                long segment = extractSegment(simHash, i);
                String bucketKey = buildSegmentKey(i, segment);
                redisTemplate.opsForSet().add(bucketKey, String.valueOf(simHash));

                // [Bug修复] 修复TTL逻辑严重错误
                // 原代码：Duration expire = Duration.ofDays(redisTemplate.getExpire(bucketKey));
                // 错误1：getExpire()返回秒数（如86400），Duration.ofDays(86400)把秒数当天数
                //        → 创建了86400天=236年的Duration，getSeconds()返回7,464,960,000 >> 0
                //        → 条件永远不成立 → TTL永远不会被设置
                // 错误2：即使修复单位转换，原逻辑也与架构要求矛盾
                //        架构要求"只在Key创建时设TTL，防高频写入刷新导致永不过期"
                //
                // 修复方案：
                //   getExpire()返回值含义：>0=剩余秒数, -1=永不过期, -2=key不存在
                //   只在key没有TTL（-1）或key不存在（-2）时设置过期时间
                //   已有TTL的key不刷新，保证TTL只在首次创建时设置
                Long ttlSeconds = redisTemplate.getExpire(bucketKey);
                if (ttlSeconds == null || ttlSeconds < 0) {
                    redisTemplate.expire(bucketKey, Duration.ofHours(TTL_HOURS));
                }
            }

            // 写入详情
            String detailKey = buildDetailKey(simHash);
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(detailKey, json, Duration.ofHours(TTL_HOURS));

            log.info("[SimHash] 写入 | hash={} | source={} | result={} | tag={} | seg={}~{}",
                    simHash, auditSource, auditResult, violationTag, segmentKeys.get(0), segmentKeys.get(3));

        } catch (Exception e) {
            log.error("[SimHash] 写入异常 | text={}", truncate(text, 50), e);
        }
    }

    public void remove(long simHash) {
        for (int i = 0; i < SEGMENTS; i++) {
            long segment = extractSegment(simHash, i);
            String bucketKey = buildSegmentKey(i, segment);
            redisTemplate.opsForSet().remove(bucketKey, String.valueOf(simHash));
        }
        redisTemplate.delete(buildDetailKey(simHash));
        log.info("[SimHash] 删除 | hash={}", simHash);
    }

    // ========== 内部方法 ==========

    private SimHashResult findSimilar(String text) {
        try {
            long simHash = SimHashUtil.computeSimHash(text);
            Set<String> candidates = new HashSet<>();
            for (int i = 0; i < SEGMENTS; i++) {
                long segment = extractSegment(simHash, i);
                String bucketKey = buildSegmentKey(i, segment);
                Set<String> members = redisTemplate.opsForSet().members(bucketKey);
                if (members != null && !members.isEmpty()) {
                    candidates.addAll(members);
                }
            }

            if (candidates.isEmpty()) return null;

            SimHashResult best = null;
            int minDist = Integer.MAX_VALUE;

            for (String hashStr : candidates) {
                try {
                    long cachedHash = Long.parseLong(hashStr);
                    String json = redisTemplate.opsForValue().get(buildDetailKey(cachedHash));
                    if (json == null) continue;

                    JsonNode node = objectMapper.readTree(json);
                    SimHashResult candidate = objectMapper.treeToValue(node, SimHashResult.class);

                    if (candidate.getAuditSource() == null && node.has("aiReason")) {
                        String aiReason = node.get("aiReason").asText();
                        candidate.setAuditSource(aiReason.contains("AI终审") ? "AI_AUDIT" : "UNKNOWN");
                        candidate.setAuditResult(aiReason.contains("：")
                                ? aiReason.split("：")[1].toUpperCase()
                                : "REVIEW");
                        candidate.setAuditComment(aiReason);
                    }

                    if (!candidate.isValid()) {
                        log.debug("[SimHash] 过期跳过 | hash={}", cachedHash);
                        continue;
                    }

                    int dist = SimHashUtil.hammingDistance(simHash, cachedHash);
                    if (dist < THRESHOLD && dist < minDist) {
                        minDist = dist;
                        best = candidate;
                    }
                } catch (Exception e) {
                    log.warn("[SimHash] 候选异常 | hashStr={}", hashStr, e);
                }
            }

            if (best != null) {
                best.setMatchDistance(minDist);
                log.info("[SimHash] 命中 | hash={} | dist={} | source={} | result={}",
                        best.getSimHash(), minDist, best.getAuditSource(), best.getAuditResult());
            }
            return best;

        } catch (Exception e) {
            log.error("[SimHash] 查询异常 | text={}", truncate(text, 50), e);
            return null;
        }
    }

    private long extractSegment(long simHash, int segmentIndex) {
        int shift = segmentIndex * BITS_PER_SEGMENT;
        return (simHash >>> shift) & 0xFFFFL;
    }

    private String buildSegmentKey(int segmentIndex, long segmentValue) {
        return PREFIX + "seg:" + segmentIndex + ":" + segmentValue;
    }

    private String buildDetailKey(long simHash) {
        return PREFIX + "detail:" + simHash;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}