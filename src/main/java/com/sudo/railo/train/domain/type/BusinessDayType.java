package com.sudo.railo.train.domain.type;

import java.time.DayOfWeek;
import java.time.LocalDate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 영업일 구분
 */
@Getter
@RequiredArgsConstructor
public enum BusinessDayType {

	WEEKDAY("1", "평일"),
	WEEKEND("2", "주말"),
	HOLIDAY("3", "공휴일");

	private final String code;
	private final String displayName;

	/**
	 * 코드로 BusinessType 반환
	 */
	public static BusinessDayType fromCode(String code) {
		return switch (code) {
			case "1" -> WEEKDAY;
			case "2" -> HOLIDAY;
			case "3" -> WEEKEND;
			default -> WEEKDAY;  // null이나 알 수 없는 코드는 평일로 처리
		};
	}

	/**
	 * 날짜로 BusinessDayType 반환
	 */
	public static BusinessDayType fromDate(LocalDate date, boolean isHoliday) {
		if (isHoliday) {
			return HOLIDAY;
		}

		DayOfWeek dayOfWeek = date.getDayOfWeek();
		if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
			return WEEKEND;
		}

		return WEEKDAY;
	}
}
