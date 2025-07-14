package com.sudo.railo.train.util;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

import org.springframework.util.StringUtils;

public class OperatingDayUtil {

	private static final String EVERY_DAY = "매일";
	private static final Map<Character, Integer> dayBitMap = Map.of(
		'월', 1,
		'화', 1 << 1,
		'수', 1 << 2,
		'목', 1 << 3,
		'금', 1 << 4,
		'토', 1 << 5,
		'일', 1 << 6
	);

	public static int toBitMask(String operatingDay) {
		if (!StringUtils.hasText(operatingDay)) {
			return 0;
		}
		if (operatingDay.equals(EVERY_DAY)) {
			return 0b1111111;
		}

		int bitmask = 0;
		for (int i = 0; i < operatingDay.length(); i++) {
			Integer bit = dayBitMap.get(operatingDay.charAt(i));
			if (bit != null) {
				bitmask |= bit;
			} else {
				throw new IllegalArgumentException("잘못된 요일 형식입니다: " + operatingDay);
			}
		}
		return bitmask;
	}

	public static boolean isOperatingDay(LocalDate date, int operatingDay) {
		// 생성 날짜 요일 (월, 화, 수 ...)
		String dayOfWeek = date.getDayOfWeek()
			.getDisplayName(TextStyle.SHORT, Locale.KOREAN);

		// 운행일 체크
		return (toBitMask(dayOfWeek) & operatingDay) != 0;
	}
}
