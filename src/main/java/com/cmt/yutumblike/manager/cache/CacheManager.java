package com.cmt.yutumblike.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CacheManager {
    private TopK hotKeyDetector;
    private Cache<String, Object> localCache;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Bean
    public TopK getHotKeyDetector() {
        hotKeyDetector = new HeavyKeeper(
                // 监控 Top 100 Key
                100,
                // 宽度
                100000,
                // 深度
                5,
                // 衰减系数
                0.92,
                // 最小出现 10 次才记录
                4
        );
        return hotKeyDetector;
    }

    @Bean
    public Cache<String, Object> localCache() {
        return localCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }


    // 辅助方法：构造复合 key
    private String buildCacheKey(String hashKey, String key) {
        return hashKey + ":" + key;
    }

    /**
     * 获取当前用户点赞的博客是否为热点数据 即判断当前用户是否经常访问该博客 或者进行恶意访问
     *
     * @param hashKey redis中用户的点赞key 例如：thumb:123
     * @param key     blog中的博客id  例如：456
     * @return
     */
    public Object get(String hashKey, String key) {
        // 构造唯一的 composite key 为thumb:123:456
        String compositeKey = buildCacheKey(hashKey, key);

        // 1. 先查本地缓存
        Object value = localCache.getIfPresent(compositeKey);
        if (value != null) {
            // 本地缓存命中
            log.info("本地缓存获取到数据 {} = {}", compositeKey, value);
            // 记录访问次数（每次访问计数 +1）
            hotKeyDetector.add(key, 1);
            return value;
        }

        // 2. 本地缓存未命中，查询 Redis
        Object redisValue = redisTemplate.opsForHash().get(hashKey, key);
        if (redisValue == null) {
            // redis也没有 则传过来的key为空
            // TODO 这里应该进行优化 当本地缓存和redis缓存都没有的时候应该去查 mysql 因为目前代码是
            //  将用户点赞数据长期放在redis 当用户量过大时可能会导致数据量过大OOM 所以应该将热点数据缓存到redis中
            //  冷点数据存到mysql 进行分类  或者可能还有更好的办法来处理这种OOM
            return null;
        }

        // 3. 记录访问（计数 +1）
        // 这里面的逻辑是将该键判断为是否是热点数据存入本地缓存 并不对redis进行操作
        AddResult addResult = hotKeyDetector.add(key, 1);

        // 4. 如果是热 Key 且不在本地缓存，则缓存数据
        if (addResult.isHotKey()) {
            localCache.put(compositeKey, redisValue);
        }

        return redisValue;
    }

    // 当本地缓存存在该key时 更新访问次数 否则直接忽略
    public void putIfPresent(String hashKey, String key, Object value) {
        String compositeKey = buildCacheKey(hashKey, key);
        Object object = localCache.getIfPresent(compositeKey);
        if (object == null) {
            return;
        }
        localCache.put(compositeKey, value);
    }


    /**
     * 【新增】定时消费淘汰队列：把被挤下 Top 100 的 Key 从本地缓存里删掉
     */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS) // 每5秒执行一次
    public void cleanExpelledKeys() {
        while (true) {
            // 从淘汰队列里拿一个 Item，非阻塞，拿不到就退出循环
            Item expelledItem = hotKeyDetector.expelled().poll();
            if (expelledItem == null) {
                break;
            }
            log.info("{}数据为淘汰队列中一员，现已被挤出 ", expelledItem);

        }
    }

    // 定时清理过期的热 Key 检测数据
    @Scheduled(fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    public void cleanHotKeys() {
        hotKeyDetector.fading();
    }

}

