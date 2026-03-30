package com.portfolio.food_api.controller;

import com.portfolio.food_api.service.DietKafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/food")
@CrossOrigin(origins = "http://localhost:8081") // Vue 포트
@RequiredArgsConstructor
public class FoodApiController {

    private final WebClient webClient;
    private final DietKafkaService dietKafkaService;

    // Django 엔드포인트 상수
    private static final String DJANGO_URL_DETECT_WEBCAM = "/food_ai/detectFoodWeb";
    private static final String DJANGO_URL_SAVE_WEBCAM = "/food_ai/detectFoodWeb_save";
    private static final String DJANGO_URL_UPLOAD = "/food_ai/detectFoodWebUpload";
    private static final String DJANGO_URL_SAVE_UPLOAD = "/food_ai/detectFoodWebUpload_save";

    private static final String CRLF = "\r\n";

    /**
     * 1. 웹캠 캡처 이미지(Base64) 판독 요청
     */
    @PostMapping("/detect/webcam")
    public Mono<String> detectWebcam(@RequestParam("org_img") String orgImg, Authentication authentication) throws IOException {
        String currentUserId = authentication.getName();
        String boundary = createBoundary();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writePart(baos, boundary, "user_id", null, null, currentUserId.getBytes(StandardCharsets.UTF_8));
        writePart(baos, boundary, "org_img", null, null, orgImg.getBytes(StandardCharsets.UTF_8));
        finishBoundary(baos, boundary);

        return sendToDjango(DJANGO_URL_DETECT_WEBCAM, boundary, baos.toByteArray());
    }

    /**
     * 2. 파일 업로드 이미지 판독 요청
     */
    @PostMapping("/detect/upload")
    public Mono<String> detectUpload(@RequestParam("mfile") MultipartFile mfile, Authentication authentication) throws IOException {
        String currentUserId = authentication.getName();
        String boundary = createBoundary();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writePart(baos, boundary, "user_id", null, null, currentUserId.getBytes(StandardCharsets.UTF_8));

        writePart(baos, boundary, "mfile", mfile.getOriginalFilename(), mfile.getContentType(), mfile.getBytes());
        finishBoundary(baos, boundary);

        return sendToDjango(DJANGO_URL_UPLOAD, boundary, baos.toByteArray());
    }

    /**
     * 3. 웹캠, 업로드 결과 DB 저장 요청
     */
    @PostMapping("/save")
    public Mono<String> saveDiet(@RequestParam("meal_time") String mealTime,
                                 @RequestParam("type") String type,
                                 @RequestParam(value = "use_kafka", defaultValue = "false") boolean useKafka,
                                 Authentication authentication) throws IOException {

        // 1. SecurityContext에서 완벽하게 ID 추출
        String userId = authentication.getName();

        // 아키텍처 A: Kafka 비동기 이벤트 큐잉 모드(대용량 트래픽 방어)
        if (useKafka) {
            dietKafkaService.sendSaveEvent(userId, mealTime, type);
            return Mono.just("{\"res_code\":\"1\", \"text\":\"[Kafka 대용량 모드] 저장 요청이 안전하게 큐에 접수되었습니다.\"}");
        }

        // 아키텍처 B: WebClient 비동기-대기 모드(UX 중심 즉각 피드백)
        else {
            String boundary = createBoundary();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            writePart(baos, boundary, "user_id", null, null, userId.getBytes(StandardCharsets.UTF_8));
            writePart(baos, boundary, "meal_time", null, null, mealTime.getBytes(StandardCharsets.UTF_8));
            finishBoundary(baos, boundary);

            String endpoint = "webcam".equals(type) ? DJANGO_URL_SAVE_WEBCAM : DJANGO_URL_SAVE_UPLOAD;

            // Django의 응답을 끝까지 기다렸다가 프론트로 반환
            return sendToDjango(endpoint, boundary, baos.toByteArray());
        }
    }

    // --- [공통 유틸리티 메소드] ---

    private String createBoundary() {
        return "TossBoundary_" + UUID.randomUUID().toString().substring(0, 10);
    }

    private void finishBoundary(ByteArrayOutputStream baos, String boundary) throws IOException {
        baos.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
    }

    private void writePart(ByteArrayOutputStream baos, String boundary, String name, String filename, String contentType, byte[] data) throws IOException {
        baos.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        String disposition = "Content-Disposition: form-data; name=\"" + name + "\"";
        if (filename != null) disposition += "; filename=\"" + filename + "\"";
        baos.write((disposition + CRLF).getBytes(StandardCharsets.UTF_8));
        if (contentType != null) baos.write(("Content-Type: " + contentType + CRLF).getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(data);
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
    }

    private Mono<String> sendToDjango(String uri, String boundary, byte[] rawBody) {
        return webClient.post()
                .uri(uri)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Content-Length", String.valueOf(rawBody.length))
                .bodyValue(rawBody)
                .retrieve()
                .bodyToMono(String.class)
                // Django 서버가 꺼져있거나 에러 발생 시 처리
                .onErrorResume(e -> {
                    log.error("Django 서버 통신 실패: {}", e.getMessage());
                    return Mono.just("{\"res_code\":\"0\", \"text\":\"AI 분석 서버가 현재 점검 중입니다. 잠시 후 다시 시도해주세요.\"}");
                });
    }
}