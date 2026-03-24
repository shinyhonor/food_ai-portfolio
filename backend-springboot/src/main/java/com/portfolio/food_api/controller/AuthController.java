package com.portfolio.food_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    // 세션을 저장하기 위한 레포지토리 추가
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String userId,
                                   @RequestParam String password,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {

        // 1. 실제 인증 수행
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userId, password)
        );

        // 2. SecurityContext에 인증 정보 설정
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // 3. [가장 중요] 세션 레포지토리에 context를 명시적으로 저장
        // 이 과정이 없으면 다음 요청에서 세션을 찾지 못해 403이 발생합니다.
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok("로그인 성공");
    }
}