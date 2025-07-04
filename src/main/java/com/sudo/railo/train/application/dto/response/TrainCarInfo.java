package com.sudo.railo.train.application.dto.response;

import com.sudo.railo.train.domain.type.CarType;

import io.swagger.v3.oas.annotations.media.Schema;

public record TrainCarInfo(

	@Schema(description = "객차 고유 ID", example = "43")
	Long id,

	@Schema(description = "객차 번호", example = "0001")
	String carNumber,

	@Schema(description = "객차 타입 (STANDARD=일반실, FIRST_CLASS=특실)", example = "STANDARD")
	CarType carType,

	@Schema(description = "전체 좌석 수", example = "56")
	Integer totalSeats,

	@Schema(description = "잔여 좌석 수", example = "45")
	int remainingSeats,

	@Schema(description = "좌석 배치 (2+2, 3+2 등)", example = "2+2")
	String seatArrangement
) {
	/**
	 * remainingSeats를 업데이트한 새로운 record 인스턴스 반환
	 */
	public TrainCarInfo withRemainingSeats(int newRemainingSeats) {
		return new TrainCarInfo(
			this.id,
			this.carNumber,
			this.carType,
			this.totalSeats,
			newRemainingSeats,
			this.seatArrangement
		);
	}
}
