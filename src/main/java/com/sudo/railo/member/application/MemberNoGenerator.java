package com.sudo.railo.member.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.sudo.railo.global.redis.RedisUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberNoGenerator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

	private final RedisUtil redisUtil;

	public String generateMemberNo() {
		String today = LocalDate.now().format(DATE_FORMATTER);
		String redisKey = "todayKey:" + today;

		Long counter = redisUtil.increment(redisKey);

		// 자정까지 남은 시간 계산
		ZonedDateTime midnightToday = ZonedDateTime.of(LocalDate.now(ZONE_ID), LocalTime.MAX, ZONE_ID);
		Instant midnightInstant = midnightToday.toInstant();

		// 자정 만료
		redisUtil.expireAt(redisKey, midnightInstant);

		String paddedCounter = String.format("%04d", counter);

		return today + paddedCounter;
	}
}

