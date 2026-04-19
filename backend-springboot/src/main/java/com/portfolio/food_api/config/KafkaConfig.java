package com.portfolio.food_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        // 1번 파라미터: 2000L(2초 대기)
        // 2번 파라미터: 10L(최대 10번 재시도)
        FixedBackOff fixedBackOff = new FixedBackOff(2000L, 10L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(fixedBackOff);

        // 만약 복구가 불가능한 에러(예: 데이터 포맷 오류)면 아예 재시도를 안 하게 막기
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }
}