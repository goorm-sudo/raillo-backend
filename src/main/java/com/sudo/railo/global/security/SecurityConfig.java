package com.sudo.railo.global.security;

import static org.springframework.security.config.http.SessionCreationPolicy.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.sudo.railo.global.redis.RedisUtil;
import com.sudo.railo.global.security.jwt.JwtAccessDeniedHandler;
import com.sudo.railo.global.security.jwt.JwtAuthenticationEntryPoint;
import com.sudo.railo.global.security.jwt.JwtFilter;
import com.sudo.railo.global.security.jwt.TokenExtractor;
import com.sudo.railo.global.security.jwt.TokenProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

	private final RedisUtil redisUtil;
	private final TokenProvider tokenProvider;
	private final TokenExtractor tokenExtractor;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// 아이디와 패스워드의 일치 여부를 확인할 때 사용하는 객체
	@Bean
	public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
		CorsConfigurationSource corsConfigurationSource) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.exceptionHandling(exception -> {
				exception.authenticationEntryPoint(jwtAuthenticationEntryPoint); // 권한 확인
				exception.accessDeniedHandler(jwtAccessDeniedHandler); // 인증된 접근 확인
			})
			.sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			// HTTP 요청에 대한 접근 권한 설정
			.authorizeHttpRequests(auth -> {
				auth.requestMatchers("/", "/auth/signup", "/auth/login").permitAll()
					.requestMatchers("/api/v1/guest/register", "/api/v1/train-schedule/**").permitAll()
					.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
					.anyRequest().authenticated();
			})
			.addFilterBefore(new JwtFilter(tokenExtractor, tokenProvider, redisUtil),
				UsernamePasswordAuthenticationFilter.class)
			.build();
	}
}
