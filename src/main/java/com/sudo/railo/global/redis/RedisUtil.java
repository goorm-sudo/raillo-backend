package com.sudo.railo.global.redis;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.sudo.railo.global.exception.error.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

	private final RedisTemplate<String, Object> objectRedisTemplate;
	private final RedisTemplate<String, String> stringRedisTemplate;

	private static final int AUTH_EXPIRATION_MINUTES = 5;
	private static final int MEMBER_NO_EXPIRATION_MINUTES = 5;

	private static final String UPDATE_EMAIL_PREFIX = "updateEmail:";

	public void save(String key, String value) {
		stringRedisTemplate.opsForValue().set(key, value);
	}

	// Redis 키 값 증가
	public Long increment(String key) {
		return stringRedisTemplate.opsForValue().increment(key);
	}

	// Redis 키 만료 시간 설정
	public void expireAt(String key, Instant expireTime) {
		stringRedisTemplate.expireAt(key, expireTime);
	}

	// Member 리프레시 토큰 저장
	public void saveMemberToken(MemberRedis memberRedis) {
		objectRedisTemplate.opsForValue().set(memberRedis.getMemberNo(), memberRedis);
	}

	// 리프레시 토큰 조회
	public String getRefreshToken(String memberNo) {
		try {
			MemberRedis memberRedis = (MemberRedis)objectRedisTemplate.opsForValue().get(memberNo);

			if (memberRedis == null) {
				log.warn("memberNo : {} 멤버 정보를 찾을 수 없습니다.", memberNo);
				return null;
			}

			return memberRedis.getRefreshToken();
		} catch (Exception e) {
			log.error("getRefreshToken error : {}", e.getMessage());
			return null;
		}
	}

	// 리프레시 토큰 삭제
	public void deleteRefreshToken(String memberNo) {
		objectRedisTemplate.delete(memberNo);
	}

	// 로그아웃 토큰 정보 저장
	public void saveLogoutToken(String accessToken, LogoutRedis logoutRedis, Long expireTime) {
		objectRedisTemplate.opsForValue().set(accessToken, logoutRedis, expireTime, TimeUnit.MILLISECONDS);
	}

	// 로그아웃 토큰 정보 조회
	public LogoutRedis getLogoutToken(String accessToken) {
		Object value = objectRedisTemplate.opsForValue().get(accessToken);
		return (LogoutRedis)value;
	}

	/* 이메일 인증 관련 */
	public void saveAuthCode(String email, String authCode) {
		String key = "authEmail:" + email;
		stringRedisTemplate.opsForValue().set(key, authCode, AUTH_EXPIRATION_MINUTES, TimeUnit.MINUTES);
	}

	public String getAuthCode(String email) {
		String key = "authEmail:" + email;
		return stringRedisTemplate.opsForValue().get(key);
	}

	public void deleteAuthCode(String email) {
		String key = "authEmail:" + email;
		stringRedisTemplate.delete(key);
	}

	/* 회원번호 찾기 관련 */
	public void saveMemberNo(String email, String memberNo) {
		String key = "memberNo:" + email;
		stringRedisTemplate.opsForValue().set(key, memberNo, MEMBER_NO_EXPIRATION_MINUTES, TimeUnit.MINUTES);
	}

	public String getMemberNo(String email) {
		String key = "memberNo:" + email;
		return stringRedisTemplate.opsForValue().get(key);
	}

	public void deleteMemberNo(String email) {
		String key = "memberNo:" + email;
		stringRedisTemplate.delete(key);
	}

	public boolean handleUpdateEmailRequest(String email) {

		// 락 전용 키
		String lockKey = "lock:" + email;

		// 중복 요청 확인용 키
		String requestKey = UPDATE_EMAIL_PREFIX + email;

		// 동일한 요청이 존재하면 false 반환
		if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(requestKey))) {
			log.error("이미 동일한 요청이 존재합니다.");
			return false;
		}

		// 락 획득 시도
		Boolean lockAcquired = stringRedisTemplate.opsForValue()
			.setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.SECONDS); //5초로 락 설정

		if (lockAcquired != null && lockAcquired) {
			try {
				saveUpdateEmailRequest(email);
			} catch (Exception e) {
				throw new BusinessException(RedisError.EMAIL_UPDATE_REQUEST_SAVE_FAIL);
			} finally {
				releaseLock(lockKey);
			}
			deleteUpdateEmailRequest(email);
			return true;
		} else {
			log.error("요청을 처리할 수 없습니다.");
			return false;
		}
	}

	private void releaseLock(String redisKey) {
		stringRedisTemplate.delete(redisKey);
	}

	private void saveUpdateEmailRequest(String email) {
		String key = UPDATE_EMAIL_PREFIX + email;
		stringRedisTemplate.opsForValue().set(key, "REQUESTED", AUTH_EXPIRATION_MINUTES, TimeUnit.MINUTES);
	}

	private void deleteUpdateEmailRequest(String email) {
		String key = UPDATE_EMAIL_PREFIX + email;
		stringRedisTemplate.delete(key);
	}

}
