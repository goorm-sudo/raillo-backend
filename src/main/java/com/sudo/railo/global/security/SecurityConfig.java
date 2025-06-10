package com.sudo.railo.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    // 필터 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 보호 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // HTTP 요청에 대한 접근 권한 설정 (초기 모두 허용)
                .authorizeHttpRequests(auth -> {
                    auth.anyRequest().permitAll();
                })
                // 세션 방식을 사용하지 않음
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                // 기본 CORS 설정 사용
                .cors(cors -> {})
                .build();
    }
}
