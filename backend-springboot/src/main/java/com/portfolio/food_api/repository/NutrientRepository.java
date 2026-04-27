package com.portfolio.food_api.repository;

import com.portfolio.food_api.model.Nutrient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutrientRepository extends JpaRepository<Nutrient, Long> {
}