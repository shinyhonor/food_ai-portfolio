package com.portfolio.food_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

/**
 * 전역 예외 처리기
 * 시스템 전반의 예외 응답 규격을 통일하고, 에러 마스킹을 방지합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 표준화된 에러 응답 규격
     */
    private record ErrorResponse(String res_code, String text, String error_detail) {}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(Exception e) {
        log.error("[Unhandled Exception] ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("0", "시스템 오류가 발생했습니다.", e.getMessage()));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleRestClientException(RestClientResponseException e) {
        log.error("[External API Error] Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse("0", "외부 서비스 연동 중 오류가 발생했습니다.", e.getStatusText()));
    }

    /**
     * [Security] 권한 부족 예외 처리 (403)
     * 인증은 되었으나 해당 자원에 접근 권한이 없는 경우에 대한 명확한 응답
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("🚫 [Access Denied] Unauthorized access attempt");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("0", "해당 작업에 대한 권한이 없습니다.", null));
    }
}