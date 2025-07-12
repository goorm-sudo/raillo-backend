package com.sudo.railo.booking.application.dto.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.querydsl.core.annotations.QueryProjection;

import lombok.Getter;

@Getter
public class ReservationProjection {

	private final Long reservationId;
	private final String reservationCode;
	private final int trainNumber;
	private final String trainName;
	private final String departureStationName;
	private final String arrivalStationName;
	private final LocalTime departureTime;
	private final LocalTime arrivalTime;
	private final LocalDate operationDate;
	private final LocalDateTime expiresAt;
	private final int standardFare;
	private final int firstClassFare;

	@QueryProjection
	public ReservationProjection(
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
		int standardFare,
		int firstClassFare
	) {
		this.reservationId = reservationId;
		this.reservationCode = reservationCode;
		this.trainNumber = trainNumber;
		this.trainName = trainName;
		this.departureStationName = departureStationName;
		this.arrivalStationName = arrivalStationName;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
		this.operationDate = operationDate;
		this.expiresAt = expiresAt;
		this.standardFare = standardFare;
		this.firstClassFare = firstClassFare;
	}
}
