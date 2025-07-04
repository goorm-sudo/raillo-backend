package com.sudo.railo.train.infrastructure;

import com.sudo.railo.train.application.TrainCarSeatInfo;

/**
 * 좌석 조회 Repository
 */
public interface SeatRepositoryCustom {

	/**
	 * 열차 객차 좌석 상세 조회
	 */
	TrainCarSeatInfo findTrainCarSeatDetail(
		Long trainCarId,
		Long trainScheduleId,
		Long departureStationId,
		Long arrivalStationId
	);
}
