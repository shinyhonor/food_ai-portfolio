package com.portfolio.food_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "USERS") // 오라클 테이블명과 일치
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberNo;

    @Column(name = "USER_ID", unique = true)
    private String userId;

    private String password; // BCrypt 암호화된 비밀번호

    private String role; // 예: "ROLE_USER"
}