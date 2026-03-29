package com.portfolio.food_api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/bank")
@RequiredArgsConstructor
public class BankController {

    // 분산 락을 제공해줄 Redis 클라이언트 주입
    private final RedissonClient redissonClient;
    private int balance = 10000;

    @GetMapping("/withdraw")
    public String withdraw() throws InterruptedException {
        // 1. "bank-account-lock" 이라는 이름의 자물쇠를 Redis에 요청
        RLock lock = redissonClient.getLock("bank-account-lock");

        try {
            // 2. 자물쇠를 획득하기 위해 최대 3초까지 대기하고, 획득하면 5초간 잠금 -> 10초로 변경 후
//            boolean isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);
            boolean isLocked = lock.tryLock(10, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                log.error("자물쇠 획득 실패! 사람이 너무 많습니다.");
                return "시스템 혼잡, 잠시 후 다시 시도해주세요.";
            }

            // ========= [안전 구역 (Critical Section)] =========
            // 이 안에는 오직 한 명(한 쓰레드)씩만 들어올 수 있습니다.
            if (balance >= 100) { // 100원 출금으로 변경
                int currentBalance = balance;
                Thread.sleep(50); // DB I/O 지연 50ms 재현
                balance = currentBalance - 100;
                log.info("출금 성공! 남은 잔액: {}", balance);
            }
            // ==================================================

        } finally {
            // 3. 내 볼일이 끝났으면 뒷사람을 위해 무조건 자물쇠를 풀어줌
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return "출금 요청 처리 완료";
    }

    @GetMapping("/balance")
    public String checkBalance() {
        return "최종 잔액: " + balance + "원";
    }

    // 테스트를 여러 번 하기 위해 잔액 초기화 API 추가
    @GetMapping("/reset")
    public String resetBalance() {
        balance = 10000;
        return "잔액 10000원으로 초기화 됨";
    }
}