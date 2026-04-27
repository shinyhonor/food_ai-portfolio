package com.portfolio.food_api.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 영양소 마스터 데이터
 * L2 Cache(Redis) 저장을 위해 Serializable을 구현했습니다.
 * 마스터 데이터 특성상 변경이 거의 없으므로 Getter만 오픈하여 불변성을 유지합니다.
 */
@Entity
@Table(name = "NUTRIENT")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Nutrient implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "NUTRIENT_ID")
    private Long nutrientId;

    private String name;
    private Double weight;
    private Double cal;
    private Double carbo;
    private Double sugars;
    private Double fat;
    private Double protein;
    private Double calcium;
    private Double phosphorus;
    private Double sodium;
    private Double potassium;
    private Double magnesium;
    private Double iron;
    private Double zinc;
    private Double cholesterol;
    private Double transfat;

    @Builder // 테스트 및 초기 예열 데이터 생성을 위해 빌더 패턴 도입
    public Nutrient(Long nutrientId, String name, Double cal) {
        this.nutrientId = nutrientId;
        this.name = name;
        this.cal = cal;
    }
}