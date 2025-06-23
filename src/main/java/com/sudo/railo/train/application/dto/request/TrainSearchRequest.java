package com.sudo.railo.train.application.dto.request;

import java.time.LocalDate;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.exception.TrainErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

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
	int passengerCount
) {

	public TrainSearchRequest {
		// Compact Constructor에서 비즈니스 로직 검증
		if (departureStationId != null && arrivalStationId != null &&
			departureStationId.equals(arrivalStationId)) {
			throw new BusinessException(TrainErrorCode.INVALID_ROUTE);
		}

		if (operationDate != null && operationDate.isAfter(LocalDate.now().plusMonths(1))) {
			throw new BusinessException(TrainErrorCode.OPERATION_DATE_TOO_FAR);
		}
	}
}
