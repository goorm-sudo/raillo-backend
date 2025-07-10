package com.sudo.railo.booking.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReservationDeleteRequest(
	@NotNull(message = "예약 ID는 필수입니다")
	Long reservationId
) {
}
