package com.sudo.railo.train.application.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "통합 열차 검색 요청 (좌석 상태 포함)")
public record TrainSearchRequest(
	@NotNull(message = "출발역을 선택해주세요")
	@Schema(description = "출발역 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	Long departureStationId,

	@NotNull(message = "도착역을 선택해주세요")
	@Schema(description = "도착역 ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
	Long arrivalStationId,

	@NotNull(message = "운행날짜를 선택해주세요")
	@FutureOrPresent(message = "운행날짜는 오늘 이후여야 합니다")
	@Schema(description = "운행 날짜", example = "2025-06-22", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDate operationDate,

	@Min(value = 1, message = "승객 수는 최소 1명이어야 합니다")
	@Max(value = 9, message = "승객 수는 최대 9명까지 가능합니다")
	@Schema(description = "승객 수", example = "4", defaultValue = "1")
	int passengerCount,

	@NotBlank(message = "출발 희망 시간을 선택해주세요")
	@Schema(description = "출발 희망 시간 (정시 단위)", example = "09",
		allowableValues = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
			"10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
			"20", "21", "22", "23"})
	@Pattern(regexp = "^([01]?[0-9]|2[0-3])$", message = "출발 시간은 00~23 사이의 정시 값이어야 합니다")
	String departureHour
) {

	/**
	 * 출발 희망 시간을 LocalTime으로 변환
	 */
	public LocalTime getDepartureTimeFilter() {
		return LocalTime.of(Integer.parseInt(departureHour), 0);
	}
}
