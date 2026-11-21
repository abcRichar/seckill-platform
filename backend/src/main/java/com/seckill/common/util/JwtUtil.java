package com.seckill.common.util;

import com.seckill.common.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具
 * - 管理端 token 2 小时，用户端 7 天（见 application.yml）
 * - 只放 userId/username/roleType，权限每次从缓存/DB 查，避免 token 过大且权限可实时收回
 */
@Component
public class JwtUtil {

    @Value("${seckill.jwt.secret}")
    private String secret;

    @Value("${seckill.jwt.admin-expire-seconds}")
    private long adminExpireSeconds;

    @Value("${seckill.jwt.user-expire-seconds}")
    private long userExpireSeconds;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // HS256 要求密钥至少 256 bit（32 字节）
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long userId, String username, String roleType) {
        long expire = "ADMIN".equals(roleType) ? adminExpireSeconds : userExpireSeconds;
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roleType", roleType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire * 1000))
                .signWith(key)
                .compact();
    }

    /**
     * 解析失败（过期、篡改、格式错误）返回 null，由拦截器决定放行还是 401
     */
    public LoginUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new LoginUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("roleType", String.class));
        } catch (Exception e) {
            return null;
        }
    }
}
