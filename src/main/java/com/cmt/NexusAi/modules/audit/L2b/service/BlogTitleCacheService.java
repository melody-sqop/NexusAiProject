package com.cmt.NexusAi.modules.audit.L2b.service;

import com.cmt.NexusAi.modules.blog.mapper.BlogMapper;
import com.cmt.NexusAi.modules.blog.model.entity.Blog;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogTitleCacheService {

    private final BlogMapper blogMapper;

    // 本地缓存：最多存1万条，1小时过期。博客标题极少修改，容忍短暂不一致
    private final Cache<Long, String> titleCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public String getTitle(Long blogId) {
        if (blogId == null) return null;
        return titleCache.get(blogId, id -> {
            log.debug("[本地缓存] 未命中blogTitle，查询DB | blogId={}", id);
            Blog blog = blogMapper.selectById(id);
            return blog != null ? blog.getTitle() : null;
        });
    }
}