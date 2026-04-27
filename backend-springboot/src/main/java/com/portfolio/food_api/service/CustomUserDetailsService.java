package com.portfolio.food_api.service;

import com.portfolio.food_api.model.User;
import com.portfolio.food_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Security Service] 사용자 정보 로드 서비스
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다: " + userId));

        // .roles() 대신 .authorities()를 사용
        // roles()는 내부적으로 "ROLE_" 접두사를 강제로 붙이려 시도하지만,
        // DB에는 이미 접두사가 포함되어 있으므로 authorities()를 통해 원본 문자열을 그대로 매핑합니다.
        return org.springframework.security.core.userdetails.User.withUsername(user.getUserId())
                .password(user.getPassword())
                .authorities(user.getRole()) // "ROLE_USER" 그대로 주입
                .build();
    }
}