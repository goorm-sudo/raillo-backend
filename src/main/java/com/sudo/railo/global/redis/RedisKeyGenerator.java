package com.sudo.railo.global.redis;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyGenerator {

	/**
	 * Redis 키 Prefix
	 * */
	private static final String MEMBER_NO_KEY_PREFIX = "member:no:email:";
	private static final String UPDATE_EMAIL_KEY_PREFIX = "member:update:email";
	private static final String EMAIL_AUTH_CODE_KEY_PREFIX = "auth:email:";
	private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refreshToken:memberNo:";
	private static final String LOGOUT_KEY_PREFIX = "auth:logout:accessToken:";

	public String generateMemberNoKey(String email) {
		return MEMBER_NO_KEY_PREFIX + email;
	}

	public String generateUpdateEmailKey(String email) {
		return UPDATE_EMAIL_KEY_PREFIX + email;
	}

	public String generateEmailAuthCodeKey(String email) {
		return EMAIL_AUTH_CODE_KEY_PREFIX + email;
	}

	public String generateRefreshTokenKey(String memberNo) {
		return REFRESH_TOKEN_KEY_PREFIX + memberNo;
	}

	public String generateLogoutTokenKey(String accessToken) {
		return LOGOUT_KEY_PREFIX + accessToken;
	}

}
