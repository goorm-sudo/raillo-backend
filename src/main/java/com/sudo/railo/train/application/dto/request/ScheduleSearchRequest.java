package com.sudo.railo.train.application.dto.request;

import java.time.LocalDate;

import com.sudo.railo.train.domain.type.CarType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScheduleSearchRequest(

	@NotNull(message = "운행 날짜는 필수입니다")
	LocalDate operationDate,

	@NotNull(message = "출발역은 필수입니다")
	Long departureStationId,

	@NotNull(message = "도착역은 필수입니다")
	Long arrivalStationId,

	@Min(value = 1, message = "승객 수는 1명 이상이어야 합니다")
	@Max(value = 9, message = "승객 수는 9명 이하여야 합니다")
	Integer passengerCount,

	CarType preferredCarType     // 선호 좌석 타입 (필터링용, nullable)
) {

	/* 생성 메서드 */
	// 컴팩트 생성자
	public ScheduleSearchRequest(LocalDate operationDate,
		Long departureStationId,
		Long arrivalStationId,
		Integer passengerCount) {
		this(operationDate, departureStationId, arrivalStationId, passengerCount, null);
	}
}
