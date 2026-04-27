package com.portfolio.food_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * S3 객체 스토리지 전담 서비스
 * 클라우드 네이티브 아키텍처의 무상태(Stateless)를 보장하기 위해 모든 파일을 외부 스토리지에서 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;
    @Value("${application.bucket.name}") private String bucketName;

    private static final String TEMP_PREFIX = "temp/";
    private static final String USER_DATA_PREFIX = "user_data/";

    /**
     * [Multipart 업로드]
     */
    public String uploadAndGetKey(MultipartFile file) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());
        return executeUpload(file.getBytes(), extension, file.getContentType());
    }

    /**
     * [Base64 업로드] 웹캠 데이터 전용
     */
    public String uploadAndGetKey(String base64Image) {
        String[] parts = base64Image.split(",");
        String imageData = parts.length > 1 ? parts[1] : parts[0];
        byte[] decodedBytes = Base64.getDecoder().decode(imageData);
        return executeUpload(decodedBytes, ".jpg", "image/jpeg");
    }

    private String executeUpload(byte[] bytes, String extension, String contentType) {
        String s3Key = TEMP_PREFIX + UUID.randomUUID() + extension;
        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName).key(s3Key).contentType(contentType).build(),
                    RequestBody.fromBytes(bytes));
            return s3Key;
        } catch (Exception e) {
            log.error("S3 Upload Failed: {}", e.getMessage());
            throw new RuntimeException("스토리지 업로드 중 네트워크 장애가 발생했습니다.");
        }
    }

    /**
     * S3 내부 네트워크를 이용한 파일 이동
     * 서버의 대역폭을 소모하지 않고 스토리지 엔진 내에서 복사를 수행합니다.
     */
    public String moveToPermanentStorage(String tempKey, String userId) {
        // 이미 영구 저장된 경로인 경우 무시
        if (tempKey.startsWith(USER_DATA_PREFIX)) return tempKey;

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = tempKey.substring(tempKey.lastIndexOf("/") + 1);
        String permanentKey = String.format("%s%s/diet/%s/%s", USER_DATA_PREFIX, userId, datePath, fileName);

        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(tempKey)
                    .destinationBucket(bucketName)
                    .destinationKey(permanentKey)
                    .build();

            s3Client.copyObject(copyRequest);
            return permanentKey;
        } catch (Exception e) {
            log.error("S3 CopyObject Failed: {}", e.getMessage());
            throw new RuntimeException("파일 영속화 과정에서 오류가 발생했습니다.");
        }
    }

    private String getFileExtension(String fileName) {
        return (fileName != null && fileName.contains(".")) ? fileName.substring(fileName.lastIndexOf(".")) : ".jpg";
    }
}