package com.portfolio.food_api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;



import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    // 1. 동기(Blocking) 방식: 전통적인 톰캣 쓰레드 물고 늘어지기
    @GetMapping("/blocking")
    public String blocking() throws InterruptedException {
        log.info("Blocking 요청 수신 - Thread: {}", Thread.currentThread().getName());
        // AI 서버가 3초 걸린다고 가정하고 쓰레드를 3초간 기절시킴
        Thread.sleep(3000);
        return "Blocking 완료";
    }

    // 2. 비동기(Non-blocking) 방식: Netty EventLoop 활용
    @GetMapping("/non-blocking")
    public Mono<String> nonBlocking() {
        log.info("Non-blocking 요청 수신 - Thread: {}", Thread.currentThread().getName());
        // 쓰레드를 기절시키지 않고, 3초 뒤에 응답하겠다는 예약(Event)만 걸어둠
        return Mono.delay(Duration.ofSeconds(3))
                .map(i -> "Non-blocking 완료");
    }
}


