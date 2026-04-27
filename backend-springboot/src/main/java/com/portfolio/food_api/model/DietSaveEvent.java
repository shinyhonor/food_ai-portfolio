package com.portfolio.food_api.model;

import com.portfolio.food_api.dto.FoodInfoResponseDto;
import java.util.List;

/**
 * [Event Model] 식단 저장 이벤트 메시지 규격
 * 가상 스레드 및 분산 환경에서 데이터의 일관성을 보장하기 위해 Java 21의 record를 사용합니다.
 * 기존 Map 기반 구조를 제거하고 정형화된 DTO 리스트를 포함하여 타입 안정성을 확보
 */
public record DietSaveEvent(
        String userId,
        String mealTime,
        String dietS3Key,
        List<FoodInfoResponseDto> foods // List<Map>에서 표준 DTO 리스트로 변경
) {
}