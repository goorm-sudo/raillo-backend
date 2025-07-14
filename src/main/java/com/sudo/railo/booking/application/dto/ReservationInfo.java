package com.sudo.railo.booking.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.sudo.railo.booking.application.dto.projection.ReservationProjection;
import com.sudo.railo.booking.application.dto.projection.SeatReservationProjection;

public record ReservationInfo(
	Long reservationId,
	String reservationCode,
	int trainNumber,
	String trainName,
	String departureStationName,
	String arrivalStationName,
	LocalTime departureTime,
	LocalTime arrivalTime,
	LocalDate operationDate,
	LocalDateTime expiresAt,
	List<SeatReservationProjection> seats
) {

	public static ReservationInfo of(
		ReservationProjection projection,
		List<SeatReservationProjection> seats
	) {
		return new ReservationInfo(
			projection.getReservationId(),
			projection.getReservationCode(),
			projection.getTrainNumber(),
			projection.getTrainName(),
			projection.getDepartureStationName(),
			projection.getArrivalStationName(),
			projection.getDepartureTime(),
			projection.getArrivalTime(),
			projection.getOperationDate(),
			projection.getExpiresAt(),
			seats
		);
	}
}
