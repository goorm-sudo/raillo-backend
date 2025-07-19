package com.sudo.railo.global.redis;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberRedisRepository {

	private final RedisTemplate<String, String> stringRedisTemplate;

	private static final Duration COMMON_EXPIRE_TIME = Duration.ofMinutes(5);

	private static final String MEMBER_NO_KEY_PREFIX = "member:no:email:";
	private static final String UPDATE_EMAIL_KEY_PREFIX = "member:update:email";

	/**
	 * 회원번호 관련
	 * Key = member:getMemberNo:email:{email}
	 * */
	public void saveMemberNo(String email, String memberNo) {
		stringRedisTemplate.opsForValue()
			.set(MEMBER_NO_KEY_PREFIX + email, memberNo, COMMON_EXPIRE_TIME);
	}

	public String getMemberNo(String email) {
		return stringRedisTemplate.opsForValue().get(MEMBER_NO_KEY_PREFIX + email);
	}

	public void deleteMemberNo(String email) {
		stringRedisTemplate.delete(MEMBER_NO_KEY_PREFIX + email);
	}

	/**
	 * 이메일 변경 관련
	 * Key = member:updateEmail:email:{email}
	 * */
	public boolean handleUpdateEmailRequest(String email) {

		String key = UPDATE_EMAIL_KEY_PREFIX + email;
		Boolean isSuccess = stringRedisTemplate.opsForValue()
			.setIfAbsent(key, "REQUESTED", COMMON_EXPIRE_TIME);
		return isSuccess != null && isSuccess;
	}

	public void deleteUpdateEmailRequest(String email) {
		stringRedisTemplate.delete(UPDATE_EMAIL_KEY_PREFIX + email);
	}
}
