package com.portfolio.food_api;

import com.portfolio.food_api.controller.BankController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [동시성 Test] 멀티 스레드 환경의 데이터 정합성 검증
 * CountDownLatch를 활용하여 100명의 동시 요청을 시뮬레이션합니다.
 */
@SpringBootTest
public class BankConcurrencyTest {

    @Autowired
    private BankController bankController;

    @Test
    @DisplayName("100명 동시 출금 테스트: Redis 분산 락을 통한 데이터 무결성 검증")
    void withdraw_concurrency_test() throws InterruptedException {
        int threadCount = 100;
        bankController.resetBalance();

        // 가상 스레드 환경에서도 작동하는 동시성 테스트 설계
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    bankController.withdraw();
                } catch (Exception e) {
                    System.err.println("Test Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 종료 대기

        String result = bankController.checkBalance();
        System.out.println("[Test Result] " + result);

        // 100번의 트랜잭션이 완벽하게 직렬화되었음을 확인
        assertThat(result).isEqualTo("최종 잔액: 0원");
    }
}