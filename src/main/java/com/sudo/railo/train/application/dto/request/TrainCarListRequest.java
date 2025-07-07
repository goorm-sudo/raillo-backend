package com.sudo.railo.train.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 잔여 좌석이 있는 열차 객차 목록 조회 요청
 */
@Schema(description = "잔여 좌석이 있는 열차 객차 목록 조회 요청")
public record TrainCarListRequest(
	@Schema(description = "열차 스케줄 ID", example = "1")
	@NotNull(message = "열차 스케줄 ID는 필수입니다")
	Long trainScheduleId,

	@Schema(description = "출발역 ID", example = "1")
	@NotNull(message = "출발역 ID는 필수입니다")
	Long departureStationId,

	@Schema(description = "도착역 ID", example = "2")
	@NotNull(message = "도착역 ID는 필수입니다")
	Long arrivalStationId,

	@Schema(description = "승객 수", example = "2")
	@Min(value = 1, message = "승객 수는 1명 이상이어야 합니다")
	@Max(value = 9, message = "승객 수는 9명 이하여야 합니다")
	int passengerCount
) {
}
