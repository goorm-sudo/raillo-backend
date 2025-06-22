package com.sudo.railo.train.application.dto.request;

import java.time.DayOfWeek;
import java.time.LocalDate;

import com.sudo.railo.global.domain.YesNo;
import com.sudo.railo.train.domain.type.BusinessDayType;

import io.swagger.v3.oas.annotations.media.Schema;

public record OperationCalendarItem(

	@Schema(description = "운행 날짜", example = "2025-06-20")
	LocalDate operationDate,

	@Schema(description = "요일 (MONDAY, TUESDAY, ...", example = "FRIDAY")
	DayOfWeek dayOfWeek,

	@Schema(description = "영업일 구분 (WEEKDAY, WEEKEND, HOLIDAY)", example = "WEEKDAY")
	BusinessDayType businessDayType,

	@Schema(description = "휴일 여부 (Y/N)", example = "N")
	String isHoliday,

	@Schema(description = "예약 가능 여부 (Y/N), 해당 날짜에 KTX 운행 스케줄 존재 여부", example = "Y")
	String isBookingAvailable
) {

	public static OperationCalendarItem create(LocalDate operationDate, boolean isHoliday, boolean hasSchedule) {
		return new OperationCalendarItem(
			operationDate,
			operationDate.getDayOfWeek(),
			determineBusinessDayType(operationDate, isHoliday),
			YesNo.from(isHoliday).getValue(),
			YesNo.from(hasSchedule).getValue()
		);
	}

	/**
	 * 영업일 구분
	 */
	private static BusinessDayType determineBusinessDayType(LocalDate date, boolean isHoliday) {
		if (isHoliday) {
			return BusinessDayType.HOLIDAY;
		}

		DayOfWeek dayOfWeek = date.getDayOfWeek();
		if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
			return BusinessDayType.WEEKEND;
		}

		return BusinessDayType.WEEKDAY;
	}
}
