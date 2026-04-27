package com.portfolio.food_api.mapper;

import com.portfolio.food_api.dto.FoodInfoResponseDto;
import com.portfolio.food_api.model.Nutrient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 객체 간 매핑 전담 인터페이스(MapStruct)
 * 가상 스레드 환경에서 Reflection 없이 컴파일 타임에 생성된 코드로 동작하여 성능을 극대화합니다.
 */
@Mapper(componentModel = "spring")
public interface NutrientMapper {

    /**
     * Entity -> DTO 변환
     * s3CropKey와 Nutrient 정보를 결합하여 정형화된 응답 객체를 생성합니다.
     */
    @Mapping(target = "img", source = "s3CropKey")
    @Mapping(target = "nutrient_id", source = "nutrient.nutrientId")
    FoodInfoResponseDto toResponseDto(Nutrient nutrient, String s3CropKey);

    /**
     * Fallback 매핑 로직(데이터 미존재 시)
     * DB에 영양소 정보가 없을 때, 서비스 코드의 생성을 방지하기 위해 매퍼 내부에 정의합니다.
     */
    default FoodInfoResponseDto toEmptyResponseDto(Long classId, String className, String s3CropKey) {
        return FoodInfoResponseDto.builder()
                .nutrient_id(classId)
                .name(className)
                .img(s3CropKey)
                .cal(0.0)
                .weight(0.0)
                .carbo(0.0)
                .protein(0.0)
                .fat(0.0)
                .sugars(0.0)
                .sodium(0.0)
                .cholesterol(0.0)
                .calcium(0.0)
                .phosphorus(0.0)
                .potassium(0.0)
                .magnesium(0.0)
                .iron(0.0)
                .zinc(0.0)
                .transfat(0.0)
                .build();
    }
}