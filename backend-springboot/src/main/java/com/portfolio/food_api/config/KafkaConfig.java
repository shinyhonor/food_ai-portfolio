package com.portfolio.food_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * [Messaging Config] Kafka 메시지 큐 예외 처리 설정
 * 시스템 간 비동기 통신 시 발생할 수 있는 일시적 장애에 대응하는 전략을 정의
 */
@Slf4j
@Configuration
public class KafkaConfig {

    /**
     * [장애 내성] Kafka Consumer 전용 에러 핸들러
     * 메시지 소비 중 에러 발생 시 시스템이 즉시 중단되지 않고 자동 복구를 시도하도록 설계했습니다.
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        // 2초(2000ms) 간격으로 최대 10번 재시도 수행
        // AI 워커 노드의 일시적 점검이나 네트워크 순단 현상을 충분히 커버
        FixedBackOff fixedBackOff = new FixedBackOff(2000L, 10L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
            // [Monitoring] 최대 재시도 횟수 초과 시 실행되는 로직
            log.error("[Kafka Error] Max retries exhausted for message: {}. Error: {}",
                    record.value(), exception.getMessage());
            // 향후 DLQ(Dead Letter Queue) 적재 로직이 추가될 예정
        }, fixedBackOff);

        // 재시도가 의미 없는 예외(Bad Request 등)는 즉시 중단하여 리소스 낭비 방지
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        errorHandler.addNotRetryableExceptions(jakarta.validation.ValidationException.class);

        return errorHandler;
    }
}