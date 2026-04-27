package com.portfolio.food_api.repository;

import com.portfolio.food_api.model.Diet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DietRepository extends JpaRepository<Diet, Long> {

    /**
     * Fetch Join 적용
     * 한 번의 쿼리로 Diet와 연관된 User를 함께 가져와 N+1 문제를 방지합니다.
     */
    @Query("select d from Diet d join fetch d.user where d.user.userId = :userId")
    List<Diet> findAllByUserIdWithUser(@Param("userId") String userId);

    /**
     * 식단 정보와 하위 음식 기록들까지 한 번에 로딩
     */
    @Query("select d from Diet d join fetch d.foodRecords fr join fetch fr.nutrient where d.dietId = :dietId")
    Optional<Diet> findByIdFullDetails(@Param("dietId") Long dietId);
}