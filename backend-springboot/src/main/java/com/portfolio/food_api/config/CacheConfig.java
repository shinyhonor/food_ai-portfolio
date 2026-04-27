package com.portfolio.food_api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * [Performance Config] 2-Level Caching 전략 설정
 * 데이터베이스 부하를 최소화하고 응답 속도를 극대화하기 위해 L1(로컬) 및 L2(분산) 캐시를 구축합니다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String NUTRIENT_CACHE = "nutrients";

    /**
     * L1 Cache: Caffeine (In-Memory)
     * 네트워크 홉이 없는 프로세스 내부 메모리를 사용하여 초저지연(Micro-second) 조회를 구현합니다.
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(NUTRIENT_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS) // 1시간 후 자동 갱신
                .maximumSize(1000)                  // 메모리 압박 방지를 위한 최대 크기 제한
                .recordStats());                    // 모니터링을 위한 통계 기록 활성화
        return cacheManager;
    }

    /**
     * L2 Cache: Redis (Distributed)
     * 여러 서버 인스턴스가 공통으로 사용하는 분산 캐시 저장소입니다.
     * L1 Cache Miss 시 데이터 일관성을 유지하는 역할을 합니다.
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1)) // 마스터 데이터 특성을 고려하여 1일 유지
                .disableCachingNullValues()   // Null 값 캐싱 방지로 메모리 절약
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}