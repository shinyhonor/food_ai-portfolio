package com.portfolio.food_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.food_api.dto.FoodInfoResponseDto;
import com.portfolio.food_api.model.DietSaveEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * [Messaging Layer] Kafka 메시징 조율 서비스
 * 비동기 작업의 발행(Publish)과 소비(Consume)를 전담합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietKafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DietSaveService dietSaveService;
    private final CacheManager caffeineCacheManager;

    private static final String SAVE_TOPIC = "diet-save-topic";
    private static final String ANALYSIS_REQ_TOPIC = "analysis-request-topic";
    private static final String CACHE_EVICT_TOPIC = "cache-evict-topic";
    private static final String TRACE_HEADER = "X-Trace-Id";

    /**
     * [Broadcasting Invalidation] 모든 서버의 L1 캐시 무효화 신호 전파
     */
    public void broadcastCacheEvict(Long nutrientId) {
        kafkaTemplate.send(CACHE_EVICT_TOPIC, nutrientId.toString());
        log.info("[Cache] Broadcasted eviction signal for ID: {}", nutrientId);
    }

    /**
     * [L1 Cache Sync] 타 인스턴스로부터 온 무효화 신호를 수신하여 로컬 메모리 동기화
     */
    @KafkaListener(topics = CACHE_EVICT_TOPIC, groupId = "#{T(java.util.UUID).randomUUID().toString()}")
    public void consumeCacheEvict(String nutrientId) {
        var cache = caffeineCacheManager.getCache("nutrients");
        if (cache != null) {
            cache.evict(Long.parseLong(nutrientId));
            log.info("[Cache] Local L1 Cache cleared for ID: {}", nutrientId);
        }
    }

    /**
     * AI 분석 요청 메시지 발행
     */
    public void sendAnalysisRequest(String userId, String s3Key) {
        try {
            Map<String, String> payload = Map.of("userId", userId, "s3_key", s3Key);
            publishWithTrace(ANALYSIS_REQ_TOPIC, payload);
        } catch (Exception e) {
            log.error("Kafka Publish Error (Analysis Request)", e);
        }
    }

    /**
     * 식단 저장 이벤트 발행
     */
    public void sendSaveEvent(String userId, String mealTime, String dietS3Key, List<FoodInfoResponseDto> foods) {
        try {
            DietSaveEvent event = new DietSaveEvent(userId, mealTime, dietS3Key, foods);
            publishWithTrace(SAVE_TOPIC, event);
        } catch (Exception e) {
            log.error("Kafka Publish Error (Save Event)", e);
        }
    }

    /**
     * 발행된 저장 이벤트를 최종 영속성 계층으로 전달
     */
    @KafkaListener(topics = SAVE_TOPIC, groupId = "food-ai-group")
    public void consumeSaveEvent(String message) {
        try {
            DietSaveEvent event = objectMapper.readValue(message, DietSaveEvent.class);
            // Record를 사용하여 필드 접근의 직관성과 불변성 확보
            dietSaveService.saveDiet(event.userId(), event.mealTime(), event.dietS3Key(), event.foods());
            log.info("[Kafka] Async DB persistence completed for user: {}", event.userId());
        } catch (Exception e) {
            log.error("[Kafka] Consumer Error (Retrying via ErrorHandler)", e);
            throw new RuntimeException("Retry trigger");
        }
    }

    private void publishWithTrace(String topic, Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, json);

        String traceId = MDC.get("traceId");
        if (traceId != null) {
            record.headers().add(TRACE_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
        }
        kafkaTemplate.send(record);
    }
}