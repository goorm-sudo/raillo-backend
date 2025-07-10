package com.sudo.railo.booking.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "예약 생성 응답 DTO")
public record ReservationCreateResponse(
	@Schema(description = "생성된 예약 ID", example = "1")
	Long reservationId,

	@Schema(description = "생성된 좌석 예약 ID", example = "[ 1, 2 ]")
	List<Long> seatReservationIds
) {
}
