package com.sudo.railo.booking.application.dto.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.train.domain.type.CarType;

import lombok.Getter;

@Getter
public class SeatReservationProjection {

	private final Long seatReservationId;
	private final Long reservationId;
	private final PassengerType passengerType;
	private final int carNumber;
	private final CarType carType;
	private final String seatNumber;

	@QueryProjection
	public SeatReservationProjection(
		Long seatReservationId,
		Long reservationId,
		PassengerType passengerType,
		int carNumber,
		CarType carType,
		String seatNumber
	) {
		this.seatReservationId = seatReservationId;
		this.reservationId = reservationId;
		this.passengerType = passengerType;
		this.carNumber = carNumber;
		this.carType = carType;
		this.seatNumber = seatNumber;
	}
}
