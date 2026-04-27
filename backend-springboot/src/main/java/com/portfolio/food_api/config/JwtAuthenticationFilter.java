package com.portfolio.food_api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청에 대해 Access Token의 유효성을 검증하며, 가상 스레드 환경에서 가볍게 동작하도록 설계
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 인증이 필요 없는 경로(Nginx verify API 등)는 토큰 검사 없이 즉시 통과
        if (request.getServletPath().equals("/api/images/verify-image")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 다중 경로(Header, Parameter)에서 토큰 추출
        String token = resolveToken(request);

        // 토큰이 존재할 경우 유효성 검증 및 컨텍스트 등록
        if (StringUtils.hasText(token)) {
            try {
                Jws<Claims> claims = jwtProvider.validateToken(token);
                String userId = claims.getPayload().getSubject();

                // Subject가 비어있는지 2차 검증
                if (StringUtils.hasText(userId)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    log.warn("[JWT] Subject field is empty in token");
                    request.setAttribute("exception", "INVALID_TOKEN");
                }
            } catch (ExpiredJwtException e) {
                log.warn("[JWT] Access Token has expired: {}", e.getMessage());
                request.setAttribute("exception", "EXPIRED_TOKEN"); // EntryPoint 전달용
            } catch (JwtException | IllegalArgumentException e) {
                log.error("[JWT] Token validation failed: {}", e.getMessage());
                request.setAttribute("exception", "INVALID_TOKEN");
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 토큰 추출 로직을 가독성 있게 분리
     */
    private String resolveToken(HttpServletRequest request) {
        // 1순위: Authorization Header (Standard API 요청)
        String bearerToken = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        // 2순위: Query Parameter (SSE / EventSource 대응)
        // 브라우저 표준 제약으로 헤더를 못 쓰는 상황을 위한 폴백(Fallback)
        String tempToken = request.getParameter("token");
        if (StringUtils.hasText(tempToken)) {
            return tempToken;
        }

        return null;
    }
}