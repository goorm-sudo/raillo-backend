package com.sudo.railo.booking.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationCreateResponse {

	private Long reservationId;
	private Long seatReservationId;
}
