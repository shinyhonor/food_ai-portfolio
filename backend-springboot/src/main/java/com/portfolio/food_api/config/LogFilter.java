package com.portfolio.food_api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * [Observability Layer] 분산 추적을 위한 Correlation ID 필터
 * 모든 요청에 고유 식별자를 부여하여 Vue -> BFF -> Kafka -> AI Worker 전 구간의 로그를 하나로 묶습니다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 최우선 순위로 설정하여 모든 필터의 로그를 추적 가능하게 함
public class LogFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 외부(Nginx 등)에서 전달된 ID가 있으면 재사용, 없으면 신규 생성
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }

        // MDC(Mapped Diagnostic Context)에 저장 -> logback-spring.xml 설정에서 [%X{traceId}]로 출력 가능
        MDC.put(MDC_KEY, correlationId);

        // 응답 헤더에 전달(클라이언트 가시성)
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startTime = System.currentTimeMillis();
        try {
            log.info("-> [Request] {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("<- [Response] Status: {}, Duration: {}ms", httpResponse.getStatus(), duration);

            // 가상 스레드 환경에서 ThreadLocal 기반의 MDC 데이터 유출 방지를 위해 반드시 명시적 제거!!!
            MDC.remove(MDC_KEY);
        }
    }
}