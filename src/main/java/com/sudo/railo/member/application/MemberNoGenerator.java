package com.sudo.railo.member.application;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberNoGenerator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("YYMMdd");

	private final StringRedisTemplate redisTemplate;

	public String generateMemberNo() {
		String today = LocalDate.now().format(DATE_FORMATTER);
		String redisKey = "memberNo:" + today;

		Long counter = redisTemplate.opsForValue().increment(redisKey);

		redisTemplate.expire(redisKey, Duration.ofDays(1));

		String paddedCounter = String.format("%04d", counter);

		return today + paddedCounter;
	}
}

