package com.sudo.railo.global.security.jwt;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.redis.RedisUtil;
import com.sudo.railo.global.security.TokenError;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// JWT 토큰을 검사하여 유효성을 확인하고, 유효한 경우 인증 정보를 설정하는 역할을 수행
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

	private final TokenExtractor tokenExtractor;
	private final TokenProvider tokenProvider;
	private final RedisUtil redisUtil;

	// 각 요청에 대해 JWT 토큰을 검사하고 유효한 경우 SecurityContext에 인증 정보를 설정
	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		
		String jwt = tokenExtractor.resolveToken(request);

		if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

			// 블랙리스트 형식으로 redis 에 해당 accessToken logout 여부 확인
			Object isLogout = redisUtil.getLogoutToken(jwt);

			// 로그아웃이 되어 있지 않은 경우 토큰 정상 작동
			if (ObjectUtils.isEmpty(isLogout)) {
				Authentication authentication = tokenProvider.getAuthentication(jwt);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} else {
				throw new BusinessException(TokenError.ALREADY_LOGOUT);
			}
		}

		filterChain.doFilter(request, response);
	}

}
