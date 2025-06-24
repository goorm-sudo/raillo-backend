package com.sudo.railo.train.application.dto.request;

import com.sudo.railo.global.domain.YesNo;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.domain.type.SeatAvailabilityStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record SeatAvailabilityItem(

	@Schema(description = "좌석 타입", example = "ECONOMY")
	String carType,

	@Schema(description = "좌석 타입명", example = "일반실")
	String carTypeName,

	@Schema(description = "잔여 좌석 수", example = "45")
	Integer availableSeats,

	@Schema(description = "총 좌석 수", example = "80")
	Integer totalSeats,

	String availabilityStatus,

	String canReserve
) {
	public static SeatAvailabilityItem create(CarType carType, Integer availableSeats, Integer totalSeats,
		SeatAvailabilityStatus status, boolean canReserve) {
		return new SeatAvailabilityItem(
			carType.name(),
			carType.getDescription(),
			availableSeats,
			totalSeats,
			status.name(),
			YesNo.from(canReserve).getValue()
		);
	}
}
