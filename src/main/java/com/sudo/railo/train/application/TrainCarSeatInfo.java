package com.sudo.railo.train.application;

import java.util.List;

import com.sudo.railo.train.application.dto.projection.SeatProjection;
import com.sudo.railo.train.domain.type.CarType;

/**
 * 객차 좌석 상세 정보
 */
public record TrainCarSeatInfo(
	String carNumber,
	CarType carType,
	String seatArrangement,
	int totalSeats,
	int remainingSeats,
	List<SeatProjection> seats
) {
	/**
	 * 좌석 배치 타입 반환 (2+2=2, 2+1=3)
	 */
	public int getLayoutType() {
		if (seatArrangement == null || seatArrangement.contains("2+2")) {
			return 2;
		} else {
			return 3;
		}
	}
}
