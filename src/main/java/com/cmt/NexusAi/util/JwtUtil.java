package com.cmt.NexusAi.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static final String SECRET_KEY_STR = "1234567890abcdefghijklmnopqrstuv12345678";
    private static final SecretKey signKey = Keys.hmacShaKeyFor(SECRET_KEY_STR.getBytes(StandardCharsets.UTF_8));

    // 过期时间改为 30 天
    private static final long EXPIRE_TIME = 30L * 24 * 60 * 60 * 1000;  // 30天

    // 生成 Token
    public static String getJwt(Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        Date nowDate = new Date(now);
        Date expireDate = new Date(now + EXPIRE_TIME);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(nowDate)
                .setExpiration(expireDate)
                .signWith(signKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 解析 Token
    public static Claims parseJwt(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(signKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }

    // 可选：检查 token 是否需要续期（剩余时间不足 3 天时续期）
    public static boolean isNeedRenew(Date expiration) {
        long remaining = expiration.getTime() - System.currentTimeMillis();
        return remaining < 3 * 24 * 60 * 60 * 1000L;  // 不足3天就续期
    }
}