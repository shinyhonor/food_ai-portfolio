package com.portfolio.food_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/food")
@CrossOrigin(origins = "http://localhost:8081") // Vue 포트
@RequiredArgsConstructor
public class FoodApiController {

    private final WebClient webClient;

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
     * 3. 웹캠 결과 DB 저장 요청
     */
    @PostMapping("/save/webcam")
    public Mono<String> saveWebcam(@RequestParam("meal_time") String mealTime, Authentication authentication) throws IOException {
        String currentUserId = authentication.getName();
        String boundary = createBoundary();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writePart(baos, boundary, "user_id", null, null, currentUserId.getBytes(StandardCharsets.UTF_8));
        writePart(baos, boundary, "meal_time", null, null, mealTime.getBytes(StandardCharsets.UTF_8));
        finishBoundary(baos, boundary);

        return sendToDjango(DJANGO_URL_SAVE_WEBCAM, boundary, baos.toByteArray());
    }

    /**
     * 4. 업로드 결과 DB 저장 요청
     */
    @PostMapping("/save/upload")
    public Mono<String> saveUpload(@RequestParam("meal_time") String mealTime, Authentication authentication) throws IOException {
        String currentUserId = authentication.getName();
        String boundary = createBoundary();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writePart(baos, boundary, "user_id", null, null, currentUserId.getBytes(StandardCharsets.UTF_8));
        writePart(baos, boundary, "meal_time", null, null, mealTime.getBytes(StandardCharsets.UTF_8));
        finishBoundary(baos, boundary);

        return sendToDjango(DJANGO_URL_SAVE_UPLOAD, boundary, baos.toByteArray());
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
                .bodyToMono(String.class);
    }
}