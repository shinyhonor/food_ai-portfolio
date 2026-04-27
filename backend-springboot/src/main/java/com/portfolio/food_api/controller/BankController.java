package com.portfolio.food_api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 동시성 제어 검증용 컨트롤러
 * Redis 분산 락(Redisson)을 활용한 데이터 정합성 보장 원리를 검증
 */
@Slf4j
@RestController
@RequestMapping("/api/bank")
@RequiredArgsConstructor
public class BankController {

    private final RedissonClient redissonClient;
    private int balance = 10000;
    private static final String LOCK_KEY = "bank-account-lock";

    @GetMapping("/withdraw")
    public String withdraw() throws InterruptedException {
        RLock lock = redissonClient.getLock(LOCK_KEY);

        try {
            // [유량 제어 전략] 10초간 대기하며 락 획득 시도, 획득 후 5초간 점유
            boolean isLocked = lock.tryLock(10, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("[Lock Timeout] Concurrent access limit exceeded.");
                return "시스템 혼잡, 잠시 후 다시 시도해주세요.";
            }

            // [Critical Section] 갱신 손실(Lost Update) 방어 구간
            if (balance >= 100) {
                int currentBalance = balance;
                Thread.sleep(50); // DB I/O 시뮬레이션
                balance = currentBalance - 100;
                log.info("[Withdraw Success] Current Balance: {}", balance);
            }

        } finally {
            // 명시적 언락을 통해 데드락 방지
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return "출금 요청 완료";
    }

    @GetMapping("/balance")
    public String checkBalance() {
        return "최종 잔액: " + balance + "원";
    }

    @GetMapping("/reset")
    public String resetBalance() {
        this.balance = 10000;
        return "잔액 초기화 완료";
    }
}