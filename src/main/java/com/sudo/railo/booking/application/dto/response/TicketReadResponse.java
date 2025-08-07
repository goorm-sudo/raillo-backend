package com.sudo.railo.booking.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.domain.type.SeatType;

public record TicketReadResponse(
	Long ticketId,
	Long reservationId,
	LocalDate operationDate,
	Long departureStationId,
	String departureStationName,
	LocalTime departureTime,
	Long arrivalStationId,
	String arrivalStationName,
	LocalTime arrivalTime,
	String trainNumber,
	String trainName,
	CarType trainCarType,
	int trainCarNumber,
	int seatRow,
	String seatColumn,
	SeatType seatType
) {
}
