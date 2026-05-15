package com.cmt.NexusAi.modules.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security 工具类：获取当前登录用户信息
 */
@Component
public class SecurityUtil {

    /**
     * 获取当前登录用户ID
     * 适配：Principal = 用户ID(Long)
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("用户未登录");
        }

        Object principal = authentication.getPrincipal();

        return (Long) principal;

    }
}