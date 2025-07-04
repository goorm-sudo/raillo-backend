package com.sudo.railo.train.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 열차 객차 좌석 상세 조회 요청
 */
@Schema(description = "열차 객차 좌석 상세 조회 요청")
public record TrainCarSeatDetailRequest(

	@Schema(description = "객차 ID", example = "1", required = true)
	@NotNull(message = "객차 ID는 필수입니다.")
	@Positive(message = "객차 ID는 양수여야 합니다.")
	Long trainCarId,

	@Schema(description = "열차 스케줄 ID", example = "1", required = true)
	@NotNull(message = "열차 스케줄 ID는 필수입니다.")
	@Positive(message = "열차 스케줄 ID는 양수여야 합니다.")
	Long trainScheduleId,

	@Schema(description = "출발역 ID", example = "1", required = true)
	@NotNull(message = "출발역 ID는 필수입니다.")
	@Positive(message = "출발역 ID는 양수여야 합니다.")
	Long departureStationId,

	@Schema(description = "도착역 ID", example = "1", required = true)
	@NotNull(message = "도착역 ID는 필수입니다.")
	@Positive(message = "도착역 ID는 양수여야 합니다.")
	Long arrivalStationId
) {
}
