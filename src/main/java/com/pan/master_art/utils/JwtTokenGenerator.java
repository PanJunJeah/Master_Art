package com.pan.master_art.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTokenGenerator {

    // 安全密钥（至少256位，实际项目中应从安全配置读取）
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Token有效期（毫秒）
    private static final long EXPIRATION_TIME = 3600000; // 1小时

    /**
     * 生成JWT Token
     * @param subject 用户标识（如用户名）
     * @param claims 自定义声明（额外信息）
     * @return JWT Token字符串
     */
    public static String generateToken(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)          // 自定义声明
                .setSubject(subject)         // 主题/用户标识
                .setIssuedAt(new Date())     // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(SECRET_KEY)        // 签名算法+密钥
                .compact();
    }

    /**
     * 解析并验证Token
     * @param token JWT Token字符串
     * @return 声明内容
     */
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static void main(String[] args) {
        // 示例用法
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("role", "admin");
        customClaims.put("userId", 12345);

        // 生成Token
        String token = generateToken("user@example.com", customClaims);
        System.out.println("Generated Token: " + token);

        // 解析验证Token
        Claims claims = parseToken(token);
        System.out.println("Subject: " + claims.getSubject());
        System.out.println("Role: " + claims.get("role"));
        System.out.println("Expiration: " + claims.getExpiration());
    }
}