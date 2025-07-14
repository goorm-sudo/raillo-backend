package com.sudo.railo.booking.application.dto.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sudo.railo.booking.domain.PassengerType;
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
	private final int fare;

	@QueryProjection
	public SeatReservationProjection(
		Long seatReservationId,
		Long reservationId,
		PassengerType passengerType,
		int carNumber,
		CarType carType,
		String seatNumber,
		int fare
	) {
		this.seatReservationId = seatReservationId;
		this.reservationId = reservationId;
		this.passengerType = passengerType;
		this.carNumber = carNumber;
		this.carType = carType;
		this.seatNumber = seatNumber;
		this.fare = fare;
	}

	public SeatReservationProjection withFare(int fare) {
		return new SeatReservationProjection(
			this.seatReservationId,
			this.reservationId,
			this.passengerType,
			this.carNumber,
			this.carType,
			this.seatNumber,
			fare
		);
	}
}
