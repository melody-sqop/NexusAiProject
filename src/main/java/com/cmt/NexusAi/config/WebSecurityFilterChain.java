package com.cmt.NexusAi.config;

import com.alibaba.fastjson2.JSON;
import com.cmt.NexusAi.common.ResultUtils;
import com.cmt.NexusAi.filter.JwtAuthenticationFilter;
import com.cmt.NexusAi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Slf4j
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityFilterChain {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private UserService userService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("=== 开始配置 Spring Security 过滤器链 ===");
        
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .userDetailsService(userService)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("🚨 认证失败触发: {}", authException.getMessage());
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(401);
                            response.getWriter().write(JSON.toJSONString(ResultUtils.error(401, "Token无效或已过期")));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("🚫 权限不足触发: {}", accessDeniedException.getMessage());
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(JSON.toJSONString(ResultUtils.error(403, "权限不足")));
                        })
                )
                .authorizeHttpRequests(auth -> {
                    auth
                            // 公开放行路径，优先级最高
                            .requestMatchers(
                                    "/user/login",
                                    "/user/register",
                                    "/user/register/**",
                                    "/user/test",
                                    "/doc.html",
                                    "/webjars/**",
                                    "/swagger-resources/**",
                                    "/v3/api-docs/**",
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/favicon.ico",
                                    "/error",
                                    "/actuator/**"
                            ).permitAll()
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        SecurityFilterChain filterChain = http.build();
        log.info("✅ Spring Security 过滤器链配置完成");
        return filterChain;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        log.info("✅ AuthenticationManager 配置完成");
        return config.getAuthenticationManager();
    }

}