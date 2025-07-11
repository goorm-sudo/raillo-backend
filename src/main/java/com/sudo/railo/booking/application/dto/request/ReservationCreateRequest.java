package com.sudo.railo.booking.application.dto.request;

import java.util.List;

import com.sudo.railo.booking.domain.PassengerSummary;
import com.sudo.railo.booking.domain.TripType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "예약 생성 요청 DTO")
public record ReservationCreateRequest(
	@Schema(description = "열차 스케줄 ID", example = "54")
	@NotNull(message = "열차 스케줄 ID는 필수입니다")
	Long trainScheduleId,

	@Schema(description = "출발역 ID", example = "2")
	@NotNull(message = "출발역 ID는 필수입니다")
	Long departureStationId,

	@Schema(description = "도착역 ID", example = "11")
	@NotNull(message = "도착역 ID는 필수입니다")
	Long arrivalStationId,

	@Schema(description = "승객 유형과 인원을 담은 오브젝트를 요소로 하는 리스트")
	@NotNull(message = "승객 정보는 필수입니다")
	List<PassengerSummary> passengers,

	@Schema(description = "좌석 ID를 요소로 하는 리스트", example = "[ 46456, 46457 ]")
	@NotNull(message = "좌석 정보는 필수입니다")
	List<Long> seatIds,

	@Schema(description = "여행 타입 (OW - 편도, RT - 왕복)", example = "OW")
	@NotNull(message = "여행 타입은 필수입니다")
	TripType tripType
) {
}
