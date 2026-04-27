package com.portfolio.food_api.service;

import com.portfolio.food_api.dto.DietAnalysisResponseDto;
import com.portfolio.food_api.dto.AiWorkerResponseDto;
import com.portfolio.food_api.dto.FoodInfoResponseDto;
import com.portfolio.food_api.mapper.NutrientMapper;
import com.portfolio.food_api.model.Nutrient;
import com.portfolio.food_api.repository.NutrientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 분석 결과와 마스터 데이터를 결합하는 핵심 엔진
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietAnalyzeService {

    private final NutrientRepository nutrientRepository;
    private final CacheManager caffeineCacheManager;
    private final CacheManager redisCacheManager;
    private final NutrientMapper nutrientMapper;

    /**
     * [Cache Warming] 서버 시작 시 영양소 마스터 데이터 예열
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        log.info("[Performance] Starting Cache Warming for Nutrients...");
        List<Nutrient> allNutrients = nutrientRepository.findAll();

        Cache l1 = caffeineCacheManager.getCache("nutrients");
        Cache l2 = redisCacheManager.getCache("nutrients");

        allNutrients.forEach(n -> {
            if (l1 != null) l1.put(n.getNutrientId(), n);
            if (l2 != null) l2.put(n.getNutrientId(), n);
        });
        log.info("[Performance] Cache Warming Complete. Count: {}", allNutrients.size());
    }

    /**
     * [2-Level Cache Search] L1 -> L2 -> DB 순차 조회 로직
     */
    public Nutrient getNutrientFromCache(Long classId) {
        Cache l1 = caffeineCacheManager.getCache("nutrients");
        Nutrient n = (l1 != null) ? l1.get(classId, Nutrient.class) : null;
        if (n != null) return n;

        log.info("🌐 [L1 Miss] Checking Redis(L2) for ID: {}", classId);
        Cache l2 = redisCacheManager.getCache("nutrients");
        n = (l2 != null) ? l2.get(classId, Nutrient.class) : null;

        if (n != null) {
            if (l1 != null) l1.put(classId, n);
            return n;
        }

        log.warn("[L2 Miss] Final Fallback to Oracle DB for ID: {}", classId);
        n = nutrientRepository.findById(classId).orElse(null);
        if (n != null) {
            if (l1 != null) l1.put(classId, n);
            if (l2 != null) l2.put(classId, n);
        }
        return n;
    }

    /**
     * 최종 응답 데이터 조립 및 영양소 계산
     */
    public DietAnalysisResponseDto buildFinalResponse(String userId, AiWorkerResponseDto workerResponse) {

        // 개별 음식 정보 변환(MapStruct 활용)
        List<FoodInfoResponseDto> foodInfoList = Optional.ofNullable(workerResponse.getDetectedFoods())
                .orElse(Collections.emptyList())
                .stream()
                .map(food -> {
                    Nutrient nutrient = getNutrientFromCache(food.getClassId());

                    // Nutrient가 있으면 정상 매핑, 없으면 Mapper의 기본값 생성기 호출
                    return (nutrient != null)
                            ? nutrientMapper.toResponseDto(nutrient, food.getCropImgKey())
                            : nutrientMapper.toEmptyResponseDto(food.getClassId(), food.getClassName(), food.getCropImgKey());
                })
                .collect(Collectors.toList());

        // 총 칼로리 집계(Java Stream API 활용)
        double totalCal = foodInfoList.stream()
                .mapToDouble(f -> f.cal() != null ? f.cal() : 0.0)
                .sum();

        // 최종 통합 응답 객체(Record) 반환
        return new DietAnalysisResponseDto(
                workerResponse.getResCode(),
                userId,
                workerResponse.getDietImgPred(),
                foodInfoList,
                Math.round(totalCal * 100.0) / 100.0
        );
    }
}