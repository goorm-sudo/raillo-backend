package com.sudo.railo.payment.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentKeyGenerator {

	private static final String KEY_PREFIX = "paymentKey:";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

	private final RedisTemplate<String, String> stringRedisTemplate;

	public String generatePaymentKey(String memberNo) {
		String today = LocalDateTime.now().format(DATE_FORMATTER);
		String redisKey = KEY_PREFIX + today;

		Long counter = stringRedisTemplate.opsForValue().increment(redisKey);

		// 키에 만료 시간이 설정되어 있지 않은 경우에만 만료 시간 설정
		Long ttl = stringRedisTemplate.getExpire(redisKey);
		if (ttl == -1) {
			ZonedDateTime midnightToday =
				ZonedDateTime.of(LocalDate.now(ZONE_ID), LocalTime.MAX, ZONE_ID);
			Instant midnightInstant = midnightToday.toInstant();

			// 자정 만료
			stringRedisTemplate.expireAt(redisKey, midnightInstant);
		}

		String convertedCounter = String.format("%03d", counter);

		return String.format("%s-%s-%s", today, memberNo, convertedCounter);
	}
}
