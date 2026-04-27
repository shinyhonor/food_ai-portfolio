package com.portfolio.food_api.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.namespace}")
    private String redisPrefix;

    private SecretKey secretKey;

    // 시간 단위를 명시적인 Duration 객체로 관리
    private static final Duration AT_EXP = Duration.ofMinutes(30);
    private static final Duration RT_EXP = Duration.ofDays(7);

    @PostConstruct
    protected void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String userId) {
        return createToken(userId, AT_EXP.toMillis());
    }

    public String createRefreshToken(String userId) {
        String refreshToken = createToken(userId, RT_EXP.toMillis());
        redisTemplate.opsForValue().set(redisPrefix + userId, refreshToken, RT_EXP.toMillis(), TimeUnit.MILLISECONDS);
        return refreshToken;
    }

    private String createToken(String userId, long expTime) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expTime))
                .signWith(secretKey)
                .compact();
    }

    public String getUserId(String token) {
        try {
            String subject = Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload().getSubject();
            return StringUtils.hasText(subject) ? subject : null;
        } catch (Exception e) {
            return null;
        }
    }

    public Jws<Claims> validateToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
    }

    /**
     * [RTR - Reuse Detection]
     * 전달된 RT가 Redis에 저장된 최신 RT와 다를 경우, 탈취된 토큰으로 간주하고 전체 세션을 무효화
     */
    public boolean validateRefreshToken(String userId, String refreshToken) {
        String savedToken = redisTemplate.opsForValue().get(redisPrefix + userId);
        if (savedToken == null) return false;

        if (!savedToken.equals(refreshToken)) {
            log.error("RTR Violation Detected! Possible Token Theft. User: {}", userId);
            redisTemplate.delete(redisPrefix + userId);
            return false;
        }
        return true;
    }
}