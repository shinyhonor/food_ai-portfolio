package com.portfolio.food_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 식단 내 개별 탐지 음식 기록
 */
@Entity
@Table(name = "FOOD_REC")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class FoodRec {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "food_rec_seq_gen")
    @SequenceGenerator(name = "food_rec_seq_gen", sequenceName = "FOOD_REC_SEQ", allocationSize = 1)
    private Long frid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIET_ID")
    private Diet diet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NUTRIENT_ID")
    private Nutrient nutrient;

    @Column(name = "FRIMG_FILENAME", length = 500)
    private String frimgFilename; // S3 Crop Object Key

    public FoodRec(Nutrient nutrient, String frimgFilename) {
        this.nutrient = nutrient;
        this.frimgFilename = frimgFilename;
    }

    /**
     * 내부 패키지에서만 호출 가능한 연관 관계 설정 메서드
     */
    void linkToDiet(Diet diet) {
        this.diet = diet;
    }
}