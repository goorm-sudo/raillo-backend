package com.sudo.railo.train.application.dto;

import com.sudo.railo.train.domain.type.CarType;

public record SeatReservationInfo(
	Long seatId,
	CarType carType,
	Long departureStationId,
	Long arrivalStationId
) {
}
