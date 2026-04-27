package com.portfolio.food_api.controller;

import com.portfolio.food_api.dto.FoodInfoResponseDto;
import com.portfolio.food_api.service.DietKafkaService;
import com.portfolio.food_api.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * [API Gateway / BFF] 서비스 통합 제어 레이어
 * 클라이언트의 요청을 수신하여 인프라(S3)와 메시징(Kafka) 레이어를 조율하는 진입점
 */
@Slf4j
@RestController
@RequestMapping("/api/food")
@CrossOrigin(origins = "http://localhost:8081")
@RequiredArgsConstructor
public class FoodApiController {

    private final S3UploadService s3UploadService;
    private final DietKafkaService dietKafkaService;

    /**
     * 웹캠 캡처 분석 요청(비동기 파이프라인)
     */
    @PostMapping("/detect/webcam")
    public ResponseEntity<Map<String, Object>> detectWebcam(@RequestParam("org_img") String base64Image,
                                                            Authentication authentication) {
        String userId = authentication.getName();
        String s3Key = s3UploadService.uploadAndGetKey(base64Image);

        // AI 워커 서버로 분석 이벤트 발행
        dietKafkaService.sendAnalysisRequest(userId, s3Key);

        return ResponseEntity.accepted().body(Map.of(
                "res_code", "1",
                "text", "분석이 시작되었습니다. 잠시 후 결과가 화면에 자동으로 표시됩니다.",
                "diet_s3_key", s3Key
        ));
    }

    /**
     * 파일 업로드 분석 요청(비동기 파이프라인)
     */
    @PostMapping("/detect/upload")
    public ResponseEntity<Map<String, Object>> detectUpload(@RequestParam("mfile") MultipartFile mfile,
                                                            Authentication authentication) throws IOException {
        String userId = authentication.getName();
        String s3Key = s3UploadService.uploadAndGetKey(mfile);

        dietKafkaService.sendAnalysisRequest(userId, s3Key);

        return ResponseEntity.accepted().body(Map.of(
                "res_code", "1",
                "text", "분석이 시작되었습니다. 잠시 후 결과가 화면에 자동으로 표시됩니다.",
                "diet_s3_key", s3Key
        ));
    }

    /**
     * 최종 식단 데이터 저장(Event-Driven Persistence)
     * 시스템의 안정성과 확장성을 위해 Kafka 기반 비동기 저장
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> saveDiet(
            @RequestParam("meal_time") String mealTime,
            @RequestParam("diet_s3_key") String dietS3Key,
            @RequestBody List<FoodInfoResponseDto> foods,
            Authentication authentication) {

        String userId = authentication.getName();

        // 직접적인 DB Insert 대신 Kafka 이벤트를 발행하여 DB 커넥션 병목을 방지하고
        // 트래픽 급증 시에도 안정적인 쓰기 처리를 보장(유량 제어)합니다.
        dietKafkaService.sendSaveEvent(userId, mealTime, dietS3Key, foods);

        return ResponseEntity.ok(Map.of(
                "res_code", "1",
                "text", "식단 저장 요청이 안전하게 접수되었습니다."
        ));
    }
}