package com.portfolio.food_api.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 식단 기록 중심 엔티티
 * 1:N 관계인 FoodRec에 대해 영속성 전이(CascadeType.ALL)를 설정하여
 * 식단 저장 시 개별 음식 기록이 원자적으로(Atomic) 함께 저장되도록 보장합니다.
 */
@Entity
@Table(name = "DIET")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Diet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "diet_seq_gen")
    @SequenceGenerator(name = "diet_seq_gen", sequenceName = "DIET_SEQ", allocationSize = 1)
    @Column(name = "DIET_ID")
    private Long dietId;

    @ManyToOne(fetch = FetchType.LAZY) // 성능을 위한 지연 로딩
    @JoinColumn(name = "MEMBER_NO")
    private User user;

    @Column(name = "IMG_FILENAME", length = 500)
    private String imgFilename; // S3 Object Key

    private String mealtime;

    @CreatedDate // JpaAuditing을 통해 DB 레벨의 생성 시간 자동 기록
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    // orphanRemoval=true: 식단에서 특정 음식 기록 제거 시 DB에서도 물리 삭제
    @OneToMany(mappedBy = "diet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodRec> foodRecords = new ArrayList<>();

    @Builder
    public Diet(User user, String imgFilename, String mealtime) {
        this.user = user;
        this.imgFilename = imgFilename;
        this.mealtime = mealtime;
    }

    /**
     * 연관 관계 편의 메소드: 양방향 객체 상태의 정합성을 보장
     */
    public void addFoodRecord(FoodRec foodRec) {
        this.foodRecords.add(foodRec);
        foodRec.linkToDiet(this);
    }
}