package com.portfolio.food_api.controller;

import com.portfolio.food_api.config.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Value("${jwt.cookie.secure}") private boolean isSecure;
    @Value("${jwt.cookie.same-site}") private String sameSite;

    /**
     * [인증] 유저 로그인 및 하이브리드 토큰 발급
     * Access Token: Response Body (JSON)
     * Refresh Token: HttpOnly Cookie (RTR 적용)
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestParam String userId,
            @RequestParam String password,
            HttpServletResponse response) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userId, password)
        );

        String at = jwtProvider.createAccessToken(auth.getName());
        String rt = jwtProvider.createRefreshToken(auth.getName());

        // 보안 쿠키 설정(HttpOnly로 XSS 방지)
        injectRefreshTokenCookie(response, rt);

        log.info("[Login] User authenticated: {}", userId);
        return ResponseEntity.ok(Map.of("accessToken", at));
    }

    /**
     * [RTR] Refresh Token을 통한 Access Token 재발급 및 RT 로테이션
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = jwtProvider.getUserId(refreshToken);

        // [Security] RTR 재사용 탐지 로직 포함 검증
        if (userId == null || !jwtProvider.validateRefreshToken(userId, refreshToken)) {
            log.warn("[RTR Alert] Refresh Token reuse detected or invalid for user: {}", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String newAt = jwtProvider.createAccessToken(userId);
        String newRt = jwtProvider.createRefreshToken(userId); // 새 RT로 교체(Rotation)

        injectRefreshTokenCookie(response, newRt);

        log.info("[Token Refresh] Tokens rotated for user: {}", userId);
        return ResponseEntity.ok(Map.of("accessToken", newAt));
    }

    private void injectRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(7 * 24 * 60 * 60) // 7일 유효
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}