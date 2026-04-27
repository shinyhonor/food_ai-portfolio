package com.portfolio.food_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * [Data Transfer Object] AI Worker 분석 결과 수신용 규격
 * 시스템 간 통신(Kafka) 시 데이터의 무결성을 보장하기 위해 필드명을 JSON 스펙과 1:1 매핑합니다.
 */
@Data
public class AiWorkerResponseDto {

    @JsonProperty("res_code")
    private String resCode;

    @JsonProperty("userId") // SSE 푸시 대상 식별을 위한 필수 필드
    private String userId;

    @JsonProperty("diet_img_pred")
    private String dietImgPred;

    @JsonProperty("detected_foods")
    private List<DetectedFood> detectedFoods;

    @Data
    public static class DetectedFood {
        @JsonProperty("class_id")
        private Long classId;

        @JsonProperty("class_name")
        private String className;

        @JsonProperty("crop_img_key")
        private String cropImgKey;
    }
}