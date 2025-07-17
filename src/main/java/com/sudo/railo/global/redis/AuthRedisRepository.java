package com.sudo.railo.global.redis;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthRedisRepository {

	private final RedisTemplate<String, String> stringRedisTemplate;
	private final RedisTemplate<String, Object> objectRedisTemplate;

	private static final int AUTH_EXPIRE_TIME = 5 * 60;
	private static final int REFRESH_TOKEN_EXPIRE_TIME = 3600 * 24 * 7;

	private static final String EMAIL_AUTH_CODE_KEY_PREFIX = "auth:email:";
	private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refreshToken:memberNo:";
	private static final String LOGOUT_KEY_PREFIX = "auth:logout:accessToken:";

	/**
	 * 이메일 인증 관련
	 * Key = auth:email:{email}
	 * */
	public void saveAuthCode(String email, String authCode) {
		stringRedisTemplate.opsForValue()
			.set(EMAIL_AUTH_CODE_KEY_PREFIX + email, authCode, AUTH_EXPIRE_TIME, TimeUnit.SECONDS);
	}

	public String getAuthCode(String email) {
		return stringRedisTemplate.opsForValue().get(EMAIL_AUTH_CODE_KEY_PREFIX + email);
	}

	public void deleteAuthCode(String email) {
		stringRedisTemplate.delete(EMAIL_AUTH_CODE_KEY_PREFIX + email);
	}

	/**
	 * RefreshToken 저장
	 * Key = auth:refreshToken:memberNo:{memberNo}
	 * */
	public void saveRefreshToken(String memberNo, String refreshToken) {
		stringRedisTemplate.opsForValue()
			.set(REFRESH_TOKEN_KEY_PREFIX + memberNo, refreshToken, REFRESH_TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
	}

	public String getRefreshToken(String memberNo) {
		return stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_KEY_PREFIX + memberNo);
	}

	public void deleteRefreshToken(String memberNo) {
		stringRedisTemplate.delete(REFRESH_TOKEN_KEY_PREFIX + memberNo);
	}

	/**
	 * Logout 관련
	 * Key = auth:logout:accessToken:{accessToken}
	 * */
	public void saveLogoutToken(String accessToken, LogoutToken logoutToken, Long expireTime) {
		objectRedisTemplate.opsForValue()
			.set(LOGOUT_KEY_PREFIX + accessToken, logoutToken, expireTime, TimeUnit.MILLISECONDS);
	}

	public LogoutToken getLogoutToken(String accessToken) {
		Object value = objectRedisTemplate.opsForValue().get(LOGOUT_KEY_PREFIX + accessToken);
		return (LogoutToken)value;
	}

}
