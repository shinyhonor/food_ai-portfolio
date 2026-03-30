package com.portfolio.food_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:9000") // Django 서버 기본 주소
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB 제한 확장
                        .build())
                .build();
    }

    // 2. DietKafkaService에서 필요로 하는 RestClient 빈 추가(동기용)
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}