package com.cmt.NexusAi.common.service;

import com.cmt.NexusAi.common.enums.ViolationTag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserScoreService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String SCORE_KEY = "user:score:%s";
    private static final String MUTE_KEY = "user:mute:%s";
    private static final String SCORED_CONTENT_KEY = "content:scored:%s";

    /** 发评论前检查：是否被禁言 */
    public boolean isMuted(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(String.format(MUTE_KEY, userId)));
    }

    /** 给用户加分（幂等：同 contentId 24h 只计一次） */
    public void addScore(Long userId, ViolationTag tag, String contentId) {
        if (tag == null || tag == ViolationTag.PASS) return;

        // 幂等：该内容是否已计分
        String scoredKey = String.format(SCORED_CONTENT_KEY, contentId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(scoredKey))) return;
        redisTemplate.opsForValue().set(scoredKey, "1", 24, TimeUnit.HOURS);

        // 累加总分
        String scoreKey = String.format(SCORE_KEY, userId);
        Long total = redisTemplate.opsForHash().increment(scoreKey, "total", tag.getScore());
        redisTemplate.opsForHash().increment(scoreKey, tag.getTag(), 1);
        redisTemplate.expire(scoreKey, 24, TimeUnit.HOURS);

        // 100分禁言 24h
        if (total != null && total >= 100 && total - tag.getScore() < 100) {
            redisTemplate.opsForValue().set(String.format(MUTE_KEY, userId), "1", 24, TimeUnit.HOURS);
            log.warn("[UserScore] 用户{}触发禁言24h | 总分={} | 标签={}", userId, total, tag.getTag());
        }
        // 300分封号 72h（简化版，如需人工复核再扩展）
        if (total != null && total >= 300 && total - tag.getScore() < 300) {
            redisTemplate.opsForValue().set(String.format("user:ban:%s", userId), "1", 72, TimeUnit.HOURS);
            log.warn("[UserScore] 用户{}触发封号72h | 总分={} | 标签={}", userId, total, tag.getTag());
        }
    }
}