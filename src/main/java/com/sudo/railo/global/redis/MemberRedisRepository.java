package com.sudo.railo.global.redis;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberRedisRepository {

	private final RedisTemplate<String, String> stringRedisTemplate;
	private final RedisKeyGenerator redisKeyGenerator;

	private static final Duration COMMON_EXPIRE_TIME = Duration.ofMinutes(5);
	
	/**
	 * 회원번호 관련
	 * Key = member:no:email:{email}
	 * */
	public void saveMemberNo(String email, String memberNo) {
		String key = redisKeyGenerator.generateMemberNoKey(email);
		stringRedisTemplate.opsForValue()
			.set(key, memberNo, COMMON_EXPIRE_TIME);
	}

	public String getMemberNo(String email) {
		String key = redisKeyGenerator.generateMemberNoKey(email);
		return stringRedisTemplate.opsForValue().get(key);
	}

	public void deleteMemberNo(String email) {
		String key = redisKeyGenerator.generateMemberNoKey(email);
		stringRedisTemplate.delete(key);
	}

	/**
	 * 이메일 변경 관련
	 * Key = member:update:email:{email}
	 * */
	public boolean handleUpdateEmailRequest(String email) {

		String key = redisKeyGenerator.generateUpdateEmailKey(email);
		Boolean isSuccess = stringRedisTemplate.opsForValue()
			.setIfAbsent(key, "REQUESTED", COMMON_EXPIRE_TIME);
		return isSuccess != null && isSuccess;
	}

	public void deleteUpdateEmailRequest(String email) {
		String key = redisKeyGenerator.generateUpdateEmailKey(email);
		stringRedisTemplate.delete(key);
	}
}
