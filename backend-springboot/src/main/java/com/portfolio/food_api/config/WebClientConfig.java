package com.portfolio.food_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HTTP 통신 클라이언트 설정
 * Java 21 HttpClient를 기반으로 하며, AI 워커와의 고성능 통신을 담당
 */
@Configuration
public class WebClientConfig {

    private static final String AI_WORKER_BASE_URL = "http://localhost:9090";

    /**
     * WebClient: 비동기/리액티브 통신용
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl(AI_WORKER_BASE_URL)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                        .build())
                .build();
    }

    /**
     * RestClient: 가상 스레드 기반 동기 통신용
     */
    @Bean
    public RestClient restClient() {
        // 하부 엔진인 java.net.http.HttpClient에서 연결 타임아웃 설정
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)) // 연결 시도 제한
                .build();

        // 설정된 클라이언트를 팩토리에 주입
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);

        // 읽기 타임아웃(응답 대기)은 팩토리 레벨에서 설정 가능
        factory.setReadTimeout(Duration.ofSeconds(30)); // AI 추론 시간 고려

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(AI_WORKER_BASE_URL)
                .build();
    }
}