package com.cmt.NexusAi.filter;

import com.alibaba.fastjson2.JSON;
import com.cmt.NexusAi.common.ResultUtils;
import com.cmt.NexusAi.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        log.info("=== JWT过滤器拦截请求 ===");
        log.info("请求URI: {}", uri);

        String token = request.getHeader("token");

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 必须把 parseJwt 包在 try 里，并且精确捕获 JJWT 异常
        try {
            Map<String, Object> claims = JwtUtil.parseJwt(token);
            Long userId = Long.valueOf(claims.get("id").toString());
            String rolesStr = (String) claims.get("roles");

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (StringUtils.hasText(rolesStr)) {
                String[] roles = rolesStr.split(",");
                for (String role : roles) {
                    if (StringUtils.hasText(role)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
                    }
                }
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("✅ JWT认证成功，用户ID：{}", userId);
            filterChain.doFilter(request, response);

        } catch (SignatureException | MalformedJwtException | ExpiredJwtException | IllegalArgumentException e) {
            // 精确捕获所有JWT错误，确保一定进这里
            log.error("❌ Token 非法：{}", e.getMessage());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(ResultUtils.error(401, "token 无效或已过期，请重新登录")));
        }
    }
}