package com.sudo.railo.train.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 열차 편성 정보
 */
@Schema(description = "열차 편성 정보")
public record TrainCompositionInfo(
	@Schema(description = "총 객차 수", example = "8")
	int totalCars,

	@Schema(description = "일반실 객차 수", example = "6")
	int standardCars,

	@Schema(description = "특실 객차 수", example = "2")
	int firstClassCars,

	@Schema(description = "총 좌석 수", example = "363")
	int totalSeats,

	@Schema(description = "일반실 좌석 수", example = "246")
	int standardSeats,

	@Schema(description = "특실 좌석 수", example = "117")
	int firstClassSeats
) {
	public static TrainCompositionInfo of(int totalCars, int standardCars, int firstClassCars,
		int totalSeats, int standardSeats, int firstClassSeats) {
		return new TrainCompositionInfo(totalCars, standardCars, firstClassCars,
			totalSeats, standardSeats, firstClassSeats);
	}
}
