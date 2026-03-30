package com.portfolio.food_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.food_api.model.DietSaveEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class DietKafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RestClient restClient;
    private final ObjectMapper objectMapper; // JSON 변환기 주입

    private static final String TOPIC_NAME = "diet-save-topic";
    private static final String DJANGO_URL = "http://localhost:9000/food_ai";

    // 1. DTO를 JSON으로 변환하여 안전하게 전송
    public void sendSaveEvent(String userId, String mealTime, String type) {
        try {
            DietSaveEvent event = new DietSaveEvent(userId, mealTime, type);
            String jsonMessage = objectMapper.writeValueAsString(event); // 객체 -> JSON String

            kafkaTemplate.send(TOPIC_NAME, jsonMessage);
            log.info("Kafka 이벤트 발행 완료: {}", jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 직렬화 실패", e);
        }
    }

    // 2. JSON을 다시 DTO로 변환하여 사용(장애 내성 유지)
    @KafkaListener(topics = TOPIC_NAME, groupId = "food-ai-group")
    public void consumeSaveEvent(String message) {
        log.info("Kafka 이벤트 수신, DB 저장 시작: {}", message);

        try {
            // JSON String -> 객체
            DietSaveEvent event = objectMapper.readValue(message, DietSaveEvent.class);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("user_id", event.getUserId());
            body.add("meal_time", event.getMealTime());

            String endpoint = "webcam".equals(event.getType()) ? "/detectFoodWeb_save" : "/detectFoodWebUpload_save";

            restClient.post()
                    .uri(DJANGO_URL + endpoint)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            log.info("DB 저장 성공!");
        } catch (Exception e) {
            log.error("DB 저장 실패, 재시도를 위해 예외 발생: {}", e.getMessage());
            throw new RuntimeException("Django 연결 실패로 인한 재시도 유도");
        }
    }
}