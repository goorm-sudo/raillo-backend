package com.sudo.railo.train.application.dto.response;

import com.sudo.railo.train.domain.type.CarType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 객차 배치 정보
 */
@Schema(description = "객차 배치 정보")
public record CarLayoutInfo(
	@Schema(description = "객차 번호", example = "1")
	int carNumber,

	@Schema(description = "객차 타입", example = "STANDARD")
	CarType carType,

	@Schema(description = "좌석 배치", example = "2+2")
	String seatArrangement,

	@Schema(description = "총 좌석 수", example = "64")
	int totalSeats,

	@Schema(description = "잔여 좌석 수", example = "32")
	int availableSeats,

	@Schema(description = "예약 가능 여부", example = "true")
	boolean isBookable
) {
	public static CarLayoutInfo of(int carNumber, CarType carType, String seatArrangement,
		int totalSeats, int availableSeats) {
		boolean isBookable = availableSeats > 0;
		return new CarLayoutInfo(carNumber, carType, seatArrangement,
			totalSeats, availableSeats, isBookable);
	}
}
