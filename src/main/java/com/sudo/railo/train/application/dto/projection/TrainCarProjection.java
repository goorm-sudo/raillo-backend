package com.sudo.railo.train.application.dto.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sudo.railo.train.application.dto.response.TrainCarInfo;
import com.sudo.railo.train.domain.type.CarType;

import lombok.Getter;
import lombok.ToString;

/**
 * QueryDSL 전용 객차 정보 Projection DTO
 */
@Getter
@ToString
public class TrainCarProjection {
	private final Long id;
	private final int carNumber;
	private final CarType carType;
	private final Integer totalSeats;
	private final int remainingSeats;
	private final String seatArrangement;

	@QueryProjection
	public TrainCarProjection(Long id, int carNumber, CarType carType,
		Integer totalSeats, int remainingSeats, String seatArrangement) {
		this.id = id;
		this.carNumber = carNumber;
		this.carType = carType;
		this.totalSeats = totalSeats;
		this.remainingSeats = remainingSeats;
		this.seatArrangement = seatArrangement;
	}

	/**
	 * remainingSeats를 업데이트한 새로운 인스턴스 반환 (불변성 유지)
	 */
	public TrainCarProjection withRemainingSeats(int newRemainingSeats) {
		return new TrainCarProjection(
			this.id,
			this.carNumber,
			this.carType,
			this.totalSeats,
			newRemainingSeats,
			this.seatArrangement
		);
	}

	/**
	 * API 응답용 record로 변환
	 */
	public TrainCarInfo toTrainCarInfo() {
		return new TrainCarInfo(
			this.id,
			String.format("%04d", this.carNumber),
			this.carType,
			this.totalSeats,
			this.remainingSeats,
			this.seatArrangement
		);
	}
}
