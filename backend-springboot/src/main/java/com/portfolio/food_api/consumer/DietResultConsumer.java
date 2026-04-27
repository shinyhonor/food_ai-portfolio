package com.portfolio.food_api.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.food_api.dto.DietAnalysisResponseDto;
import com.portfolio.food_api.dto.AiWorkerResponseDto;
import com.portfolio.food_api.controller.NotificationController;
import com.portfolio.food_api.service.DietAnalyzeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * [Messaging Layer] AI 분석 결과 수신 및 실시간 푸시 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DietResultConsumer {

    private final DietAnalyzeService dietAnalyzeService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "analysis-result-topic", groupId = "food-ai-result-group")
    public void consumeAnalysisResult(String message,
                                      @Header(name = "X-Trace-Id", required = false) byte[] traceIdBytes) {

        if (traceIdBytes != null) {
            MDC.put("traceId", new String(traceIdBytes));
        }

        try {
            log.info("[Kafka] Received AI results. Starting synthesis.");

            // AI 워커로부터 온 Raw 데이터 파싱
            AiWorkerResponseDto rawResponse = objectMapper.readValue(message, AiWorkerResponseDto.class);
            String userId = rawResponse.getUserId();

            // [BFF] 영양소 데이터 결합(DTO 반환)
            DietAnalysisResponseDto finalDto = dietAnalyzeService.buildFinalResponse(userId, rawResponse);

            // 실시간 해당 유저의 SSE 세션을 찾아 결과 푸시
            SseEmitter emitter = NotificationController.emitters.get(userId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("analysis-result")
                            .data(finalDto) // DTO 객체 전송
                            .reconnectTime(5000));
                    log.info("[SSE] Successfully pushed results to user: {}", userId);
                } catch (IOException | IllegalStateException e) {
                    log.warn("[SSE] Client disconnected. Cleaning up emitter for user: {}", userId);
                    NotificationController.emitters.remove(userId);
                }
            } else {
                log.warn("[SSE] No active session for user: {}. Results will be logged but not pushed.", userId);
            }

        } catch (Exception e) {
            log.error("[Consumer Error] Failed to process AI result message", e);
        } finally {
            MDC.clear();
        }
    }
}