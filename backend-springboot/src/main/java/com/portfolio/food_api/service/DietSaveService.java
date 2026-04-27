package com.portfolio.food_api.service;

import com.portfolio.food_api.dto.FoodInfoResponseDto;
import com.portfolio.food_api.model.*;
import com.portfolio.food_api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 식단 데이터 영속성 전담 서비스
 * S3 리소스의 물리적 이동과 DB 트랜잭션을 조율합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietSaveService {

    private final DietRepository dietRepository;
    private final UserRepository userRepository;
    private final NutrientRepository nutrientRepository;
    private final S3UploadService s3UploadService;

    /**
     * 식단 저장 프로세스
     * 1단계: 외부 네트워크 I/O(S3 복사) - 트랜잭션 외부에서 수행하여 DB 커넥션 점유 최소화
     * 2단계: DB 데이터 기록 - 짧은 트랜잭션으로 원자성 보장
     */
    public void saveDiet(String userId, String mealTime, String tempS3Key, List<FoodInfoResponseDto> foods) {

        log.info("[Storage] Starting S3 migration for user: {}", userId);

        // 원본 이미지 영구 저장소로 이동
        String permanentS3Key = s3UploadService.moveToPermanentStorage(tempS3Key, userId);

        // 개별 음식 크롭 이미지들 이동 및 데이터 매핑
        // 데이터 가공(I/O)을 먼저 완료한 뒤 DB에 진입하는 것이 장애 전파 방지의 핵심
        List<FoodRecordDto> foodRecords = foods.stream()
                .map(f -> {
                    String permCropKey = s3UploadService.moveToPermanentStorage(f.img(), userId);
                    return new FoodRecordDto(f.nutrient_id(), permCropKey);
                })
                .toList();

        // 실제 영속화 작업 수행
        executePersistence(userId, mealTime, permanentS3Key, foodRecords);
    }

    /**
     * 실제 DB Insert를 수행하는 임계 구역
     */
    @Transactional
    public void executePersistence(String userId, String mealTime, String imgKey, List<FoodRecordDto> foodRecords) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다: " + userId));

        Diet diet = Diet.builder()
                .user(user)
                .imgFilename(imgKey)
                .mealtime(mealTime)
                .build();

        for (FoodRecordDto record : foodRecords) {
            Nutrient nutrient = nutrientRepository.findById(record.nutrientId())
                    .orElseThrow(() -> new RuntimeException("마스터 데이터(영양소)가 존재하지 않습니다: " + record.nutrientId()));

            diet.addFoodRecord(new FoodRec(nutrient, record.permanentKey()));
        }

        dietRepository.save(diet);
        log.info("[DB] Diet and FoodRecords successfully persisted.");
    }

    /**
     * 내부 데이터 전송용 가벼운 Record
     */
    private record FoodRecordDto(Long nutrientId, String permanentKey) {}
}