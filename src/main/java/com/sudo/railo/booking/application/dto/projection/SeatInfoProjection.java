package com.sudo.railo.booking.application.dto.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.train.domain.Seat;

import lombok.Getter;

@Getter
public class SeatInfoProjection {

	private final Seat seat;
	private final PassengerType passengerType;

	@QueryProjection
	public SeatInfoProjection(Seat seat, PassengerType passengerType) {
		this.seat = seat;
		this.passengerType = passengerType;
	}
}
