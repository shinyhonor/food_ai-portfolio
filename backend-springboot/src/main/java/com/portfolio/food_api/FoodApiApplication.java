package com.portfolio.food_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * [Main Entry] Food-AI BFF(Backend For Frontend) Application
 * Java 21 가상 스레드 기반의 고성능 API 게이트웨이 역할을 수행하며,
 * JPA Auditing 활성화를 통해 데이터 생성 주기를 자동 관리합니다.
 */
@EnableJpaAuditing
@SpringBootApplication
public class FoodApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(FoodApiApplication.class, args);
	}
}