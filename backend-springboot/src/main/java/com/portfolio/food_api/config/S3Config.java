package com.portfolio.food_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 객체 스토리지 설정
 * 클라우드 네이티브 아키텍처의 핵심인 무상태(Stateless) 파일 관리를 위해
 * AWS S3 표준 프로토콜을 준수하는 MinIO를 연동합니다.
 */
@Configuration
public class S3Config {

    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    /**
     * S3 통신을 위한 기본 클라이언트 빈 등록
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                // [Security & Compatibility]
                // PathStyleAccessEnabled: MinIO와 같은 온프레미스 S3 호환 스토리지는
                // 가상 호스트 방식(bucket.domain.com)이 아닌 경로 방식(domain.com/bucket)을 사용해야 하므로 필수 설정
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /**
     * [Security] Presigned URL 생성을 위한 전용 클라이언트
     * Nginx의 auth_request와 결합하여 이미지 보안 접근을 제어할 때 사용됩니다.
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}