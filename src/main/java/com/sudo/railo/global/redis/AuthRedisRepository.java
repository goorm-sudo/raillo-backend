package com.sudo.railo.global.redis;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthRedisRepository {

	private final RedisTemplate<String, String> stringRedisTemplate;
	private final RedisTemplate<String, Object> objectRedisTemplate;
	private final RedisKeyGenerator redisKeyGenerator;

	private static final Duration AUTH_EXPIRE_TIME = Duration.ofMinutes(5);
	private static final Duration REFRESH_TOKEN_EXPIRE_TIME = Duration.ofDays(7);

	/**
	 * 이메일 인증 관련
	 * Key = auth:email:{email}
	 * */
	public void saveAuthCode(String email, String authCode) {
		String key = redisKeyGenerator.generateEmailAuthCodeKey(email);
		stringRedisTemplate.opsForValue()
			.set(key, authCode, AUTH_EXPIRE_TIME);
	}

	public String getAuthCode(String email) {
		String key = redisKeyGenerator.generateEmailAuthCodeKey(email);
		return stringRedisTemplate.opsForValue().get(key);
	}

	public void deleteAuthCode(String email) {
		String key = redisKeyGenerator.generateEmailAuthCodeKey(email);
		stringRedisTemplate.delete(key);
	}

	/**
	 * RefreshToken 저장
	 * Key = auth:refreshToken:memberNo:{memberNo}
	 * */
	public void saveRefreshToken(String memberNo, String refreshToken) {
		String key = redisKeyGenerator.generateRefreshTokenKey(memberNo);
		stringRedisTemplate.opsForValue()
			.set(key, refreshToken, REFRESH_TOKEN_EXPIRE_TIME);
	}

	public String getRefreshToken(String memberNo) {
		String key = redisKeyGenerator.generateRefreshTokenKey(memberNo);
		return stringRedisTemplate.opsForValue().get(key);
	}

	public void deleteRefreshToken(String memberNo) {
		String key = redisKeyGenerator.generateRefreshTokenKey(memberNo);
		stringRedisTemplate.delete(key);
	}

	/**
	 * Logout 관련
	 * Key = auth:logout:accessToken:{accessToken}
	 * */
	public void saveLogoutToken(String accessToken, LogoutToken logoutToken, Duration expireTime) {
		String key = redisKeyGenerator.generateLogoutTokenKey(accessToken);
		objectRedisTemplate.opsForValue()
			.set(key, logoutToken, expireTime);
	}

	public LogoutToken getLogoutToken(String accessToken) {
		String key = redisKeyGenerator.generateLogoutTokenKey(accessToken);
		Object value = objectRedisTemplate.opsForValue().get(key);
		return (LogoutToken)value;
	}

}
