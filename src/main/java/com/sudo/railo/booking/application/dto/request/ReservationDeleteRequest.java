package com.sudo.railo.booking.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationDeleteRequest {

	@NotNull(message = "예약 ID는 필수입니다")
	private Long reservationId;
}
