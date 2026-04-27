package com.portfolio.food_api.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 이미지 접근을 위한 단기 서명 토큰(Ticket) 발급 및 검증 엔진
 * Nginx의 auth_request와 결합하여 정적 자원에 대한 세분화된 권한 부여 작업을 수행합니다.
 */
@Slf4j
@Service
public class ImageTokenService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey secretKey;
    private static final long TOKEN_VALIDITY_MS = 5 * 60 * 1000; // 5분 유지
    private static final String IMAGE_PATH_PREFIX = "/images/";

    @PostConstruct
    protected void init() {
        // 서버 재시작 후에도 토큰 유효성을 유지하기 위해 설정된 Secret 기반으로 키 생성
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 특정 이미지 파일에 대한 임시 접근 권한을 증명하는 JWT 티켓 발급
     */
    public String generateImageToken(String userId, String s3Key) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(userId)
                .claim("file", s3Key) // 토큰 내부에 허용된 파일 경로를 넣음(URL 변조 방지)
                .issuedAt(new Date(now))
                .expiration(new Date(now + TOKEN_VALIDITY_MS))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Nginx로부터 전달받은 티켓과 요청 URI의 정합성을 검증(Stateless 인가)
     */
    public boolean verifyTokenAndOwnership(String token, String requestFileUri) {
        try {
            Jws<Claims> jwsClaims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            String userIdFromToken = jwsClaims.getPayload().getSubject();
            String fileFromToken = (String) jwsClaims.getPayload().get("file");

            // Nginx 호환성을 위해 접두어 제거 및 경로 정규화
            String normalizedUri = requestFileUri.startsWith(IMAGE_PATH_PREFIX)
                    ? requestFileUri.substring(IMAGE_PATH_PREFIX.length())
                    : requestFileUri;

            // 토큰에 명시된 파일과 실제 요청된 파일이 일치하는지 확인(Path Traversal 방어)
            if (!normalizedUri.equals(fileFromToken)) {
                log.warn("[Auth Violation] Token Path Mismatch! User: {}, Req: {}, Token: {}",
                        userIdFromToken, normalizedUri, fileFromToken);
                return false;
            }

            // 소유권 확인(필요 시 Redis/DB 조회 레이어 확장 지점)
            return checkOwnership(userIdFromToken, normalizedUri);

        } catch (Exception e) {
            log.error("[Ticket Validation Failed] {}", e.getMessage());
            return false;
        }
    }

    private boolean checkOwnership(String userId, String fileUri) {
        // 현재는 모든 인증된 유저의 접근을 허용하나,
        // 향후 user_data/ 하위 경로의 경우 userId 매칭 로직을 여기서 수행함.
        return true;
    }
}