package com.sudo.railo.booking.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "예약 정보")
public record ReservationDetail(

	@Schema(description = "예약 ID", example = "1")
	Long reservationId,

	@Schema(description = "예약 코드", example = "202507110020301F8C")
	String reservationCode,

	@Schema(description = "열차 번호", example = "027")
	String trainNumber,

	@Schema(description = "열차명", example = "KTX")
	String trainName,

	@Schema(description = "출발역명", example = "서울")
	String departureStationName,

	@Schema(description = "도착역명", example = "천안아산")
	String arrivalStationName,

	@Schema(description = "출발 시간", example = "11:58")
	LocalTime departureTime,

	@Schema(description = "도착 시간", example = "12:38")
	LocalTime arrivalTime,

	@Schema(description = "운행일", example = "2025-07-01")
	LocalDate operationDate,

	@Schema(description = "만료시간", example = "2025-07-01 17:38:59.984686")
	LocalDateTime expiresAt,

	@Schema(description = "예약 좌석 정보")
	List<SeatReservationDetail> seats
) {

	public static ReservationDetail of(
		Long reservationId,
		String reservationCode,
		String trainNumber,
		String trainName,
		String departureStationName,
		String arrivalStationName,
		LocalTime departureTime,
		LocalTime arrivalTime,
		LocalDate operationDate,
		LocalDateTime expiresAt,
		List<SeatReservationDetail> seats
	) {
		return new ReservationDetail(
			reservationId,
			reservationCode,
			trainNumber,
			trainName,
			departureStationName,
			arrivalStationName,
			departureTime,
			arrivalTime,
			operationDate,
			expiresAt,
			seats
		);
	}
}
