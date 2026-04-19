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

@SpringBootTest
public class BankConcurrencyTest {

    @Autowired
    private BankController bankController;

    @Test
    @DisplayName("100명의 사용자가 동시에 100원씩 출금하면, 잔액은 정확히 0원이 되어야 한다.")
    void withdraw_concurrency_test() throws InterruptedException {
        // 1. 준비
        int threadCount = 100;
        bankController.resetBalance(); // 잔액을 10,000원으로 초기화

        // 32개의 쓰레드를 가진 풀을 생성하여 100명의 유저를 흉내 냄
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 100개의 쓰레드가 모두 끝날 때까지 메인 쓰레드가 기다리게 해주는 장치
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 2. 실행 - 100번의 출금 요청을 동시에 쓰레드 풀에 던짐
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    bankController.withdraw(); // Redis 락이 걸려있는 출금 로직 호출
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown(); // 쓰레드 하나가 끝날 때마다 카운트 -1
                }
            });
        }

        // 모든 쓰레드가 끝날 때까지 대기(CountDownLatch가 0이 될 때까지)
        latch.await();

        // 3. 검증
        String result = bankController.checkBalance();
        System.out.println("테스트 종료 후 " + result);

        // 잔액이 정확히 0원인지 검증!
        assertThat(result).isEqualTo("최종 잔액: 0원");
    }
}