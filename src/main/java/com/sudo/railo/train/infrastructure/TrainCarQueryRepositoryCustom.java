package com.sudo.railo.train.infrastructure;

import java.util.List;

import com.sudo.railo.train.application.dto.response.TrainCarInfo;

public interface TrainCarQueryRepositoryCustom {

	/**
	 * 열차 객차 목록 조회 (잔여 좌석이 있는 객차만)
	 */
	List<TrainCarInfo> findAvailableTrainCars(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId);

	/**
	 * 특정 객차의 좌석 상세 정보 조회
	 */
/*	TrainCarSeatInfo findTrainCarSeatDetail(Long trainCarId, Long trainScheduleId, Long departureStationId,
		Long arrivalStationId);*/
}
