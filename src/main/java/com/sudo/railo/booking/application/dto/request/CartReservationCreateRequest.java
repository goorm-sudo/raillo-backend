package com.sudo.railo.booking.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "장바구니에 예약 추가 요청")
public record CartReservationCreateRequest(

	@Schema(description = "예약 ID", example = "1")
	@NotNull(message = "예약 ID는 필수입니다")
	Long reservationId
) {
}
