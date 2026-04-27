package com.portfolio.food_api.dto;

import java.util.List;

/**
 * [Response DTO] AI 분석 전체 결과 응답 객체
 * Java 21 Record를 사용하여 데이터 불변성을 보장
 */
public record DietAnalysisResponseDto(
        String res_code,
        String userId,
        String diet_img_pred,
        List<FoodInfoResponseDto> foodInfo,
        double totalCal
) {
    // 10년 차 팁: 생성 시점에 null 방어나 데이터 정규화가 필요하다면
    // Compact Constructor를 추가할 수 있습니다.
}