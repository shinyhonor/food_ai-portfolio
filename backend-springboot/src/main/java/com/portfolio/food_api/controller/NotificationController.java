package com.portfolio.food_api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 알림 전송을 위한 SSE(Server-Sent Events) 컨트롤러
 * 사용자와 서버 간의 단방향 실시간 통로를 관리합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    // [Thread-Safe] 대규모 접속 환경을 고려하여 고성능 동시성 맵 사용
    public static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 클라이언트의 실시간 결과 수신을 위한 구독 엔드포인트
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication authentication) {
        // [Security] 가상 스레드 환경에서도 SecurityContext를 통해 안전하게 인증 객체 식별
        if (authentication == null) {
            log.error("[SSE] Unauthorized access attempt detected");
            return null;
        }

        String userId = authentication.getName();

        // [Config] 기본 타임아웃 1시간 설정(추후 인프라 부하에 따라 30분~1시간 사이 조절)
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
        emitters.put(userId, emitter);

        // 연결 수립 시 즉시 초기 데이터 발송(503 에러 방지 및 연결 확인용)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected")
                    .comment("SSE Connection Established"));
            log.info("[SSE] Connection established for user: {}", userId);
        } catch (IOException e) {
            emitters.remove(userId);
        }

        // 리소스 회수를 위한 콜백 정의
        emitter.onCompletion(() -> {
            log.debug("[SSE] Stream completed for user: {}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.warn("[SSE] Stream timed out for user: {}", userId);
            emitters.remove(userId);
        });

        emitter.onError((e) -> {
            log.error("[SSE] Stream error for user: {}: {}", userId, e.getMessage());
            emitters.remove(userId);
        });

        return emitter;
    }
}