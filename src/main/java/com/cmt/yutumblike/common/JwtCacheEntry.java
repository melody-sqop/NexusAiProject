package com.cmt.yutumblike.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Data
@AllArgsConstructor
public class JwtCacheEntry {
    private Long userId;
    private String rolesStr;
    private List<SimpleGrantedAuthority> authorities;
}