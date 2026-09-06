package com.LinkedKnowledge.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key = Keys.hmacShaKeyFor(
        "linkedknowledge-jwt-secret-key-256bits!!".getBytes()
    );

    private final long expiration = 86400000; // 24小时
    public String getUsernameFromToken(String token) {
        return parseToken(token).get("username", String.class);
    }


    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }



    // 生成 token
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    // 解析 token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 从 token 取用户 ID
    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    // 取token的过期时间
    public Date getExpiration(String token) {
        return parseToken(token).getExpiration();
    }
}
