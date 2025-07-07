package com.sudo.railo.train.application.dto.response;

import java.util.List;

import com.sudo.railo.train.domain.type.CarType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 열차 객차 좌석 상세 조회 응답
 */
public record TrainCarSeatDetailResponse(

	@Schema(description = "객차 번호", example = "17")
	String carNumber,

	@Schema(description = "객차 타입", example = "STANDARD")
	CarType carType,

	@Schema(description = "전체 좌석 수", example = "56")
	int totalSeatCount,

	@Schema(description = "잔여 좌석 수", example = "41")
	int remainingSeatCount,

	@Schema(description = "좌석 배치 타입 (2=2+2 배치, 3=2+1 배치)", example = "2")
	int layoutType,

	@Schema(description = "좌석 상세 정보 목록")
	List<SeatDetail> seatList
) {

	public static TrainCarSeatDetailResponse of(
		String carNumber,
		CarType carType,
		int totalSeatCount,
		int remainingSeatCount,
		int layoutType,
		List<SeatDetail> seatList
	) {
		return new TrainCarSeatDetailResponse(
			carNumber,
			carType,
			totalSeatCount,
			remainingSeatCount,
			layoutType,
			seatList
		);
	}
}
