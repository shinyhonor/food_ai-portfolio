package com.portfolio.food_api.controller;

import com.portfolio.food_api.service.ImageTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [Security Layer] 이미지 자원 접근 인가 컨트롤러
 * Nginx의 auth_request 모듈과 협력하여 무상태 정적 자원 보호를 수행합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageTicketController {

    private final ImageTokenService imageTokenService;

    private static final String X_IMG_TOKEN = "X-Image-Token";
    private static final String X_IMG_PATH = "X-Image-Path";

    /**
     * 한 페이지 내의 여러 이미지에 대해 한 번의 네트워크 호출로 모든 보안 토큰을 발급받습니다.
     */
    @PostMapping("/ticket/bulk")
    public ResponseEntity<Map<String, String>> requestBulkTickets(
            @RequestBody List<String> s3Keys,
            Authentication authentication) {

        String userId = authentication.getName();

        Map<String, String> ticketMap = s3Keys.stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> imageTokenService.generateImageToken(userId, key)
                ));

        log.info("[Ticket] Issued {} tickets for user: {}", s3Keys.size(), userId);
        return ResponseEntity.ok(ticketMap);
    }

    /**
     * Nginx Internal Callback
     * HTTP Header에 담긴 토큰과 경로의 정합성을 검증합니다.
     */
    @GetMapping("/verify-image")
    public ResponseEntity<Void> verifyImage(
            @RequestHeader(value = X_IMG_TOKEN, required = false) String token,
            @RequestHeader(value = X_IMG_PATH, required = false) String fileUri) {

        if (token == null || fileUri == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (imageTokenService.verifyTokenAndOwnership(token, fileUri)) {
            return ResponseEntity.ok().build();
        } else {
            log.warn("[Security] Unauthorized image access blocked: {}", fileUri);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}