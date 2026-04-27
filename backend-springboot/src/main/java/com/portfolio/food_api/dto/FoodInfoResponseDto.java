package com.portfolio.food_api.dto;

import lombok.Builder;

/**
 * [Response DTO] 개별 탐지 음식의 상세 영양 정보 응답 객체
 * MapStruct와의 호환 및 명확한 타입 정의를 위해 별도 파일로 분리
 */
@Builder // 💡 MapStruct와 연동을 위해 Builder 지원
public record FoodInfoResponseDto(
        String img, // S3 Crop Key
        Long nutrient_id,
        String name,
        Double weight,
        Double cal,
        Double carbo,
        Double sugars,
        Double fat,
        Double protein,
        Double calcium,
        Double phosphorus,
        Double sodium,
        Double potassium,
        Double magnesium,
        Double iron,
        Double zinc,
        Double cholesterol,
        Double transfat
) {
}