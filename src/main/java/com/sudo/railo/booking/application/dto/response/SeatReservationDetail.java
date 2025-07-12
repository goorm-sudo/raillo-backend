package com.sudo.railo.booking.application.dto.response;

import com.sudo.railo.booking.domain.PassengerType;
import com.sudo.railo.train.domain.type.CarType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "예약 좌석 정보")
public record SeatReservationDetail(

	@Schema(description = "예약 좌석 ID", example = "1")
	Long seatReservationId,

	@Schema(description = "승객 유형", example = "ADULT")
	PassengerType passengerType,

	@Schema(description = "객차 번호", example = "1")
	int carNumber,

	@Schema(description = "객차 타입 (STANDARD=일반실, FIRST_CLASS=특실)", example = "STANDARD")
	CarType carType,

	@Schema(description = "좌석 번호 (행 + 열)", example = "1D")
	String seatNumber,

	@Schema(description = "기본 요금 (원)", example = "59800")
	int baseFare,

	@Schema(description = "요금 (원)", example = "29900")
	int fare
) {

	public static SeatReservationDetail of(
		Long seatReservationId,
		PassengerType passengerType,
		int carNumber,
		CarType carType,
		String seatNumber,
		int baseFare,
		int fare
	) {
		return new SeatReservationDetail(
			seatReservationId,
			passengerType,
			carNumber,
			carType,
			seatNumber,
			baseFare,
			fare
		);
	}
}
