package com.sudo.railo.train.infrastructure;

import java.util.List;
import java.util.Map;

import com.sudo.railo.train.application.dto.SeatReservationInfo;

public interface SeatReservationRepositoryCustom {

	/**
	 * 여러 열차의 겹치는 예약 정보를 일괄 조회
	 * @param trainScheduleIds 조회할 열차 스케줄 ID 목록
	 * @param departureStationId 출발역 ID
	 * @param arrivalStationId 도착역 ID
	 * @return Map<열차스케줄ID, 예약정보리스트>
	 */
	Map<Long, List<SeatReservationInfo>> findOverlappingReservationsBatch(
		List<Long> trainScheduleIds,
		Long departureStationId,
		Long arrivalStationId
	);

	/**
	 * 여러 열차의 입석 예약 수를 일괄 조회
	 * @param trainScheduleIds 조회할 열차 스케줄 ID 목록
	 * @param departureStationId 출발역 ID
	 * @param arrivalStationId 도착역 ID
	 * @return Map<열차스케줄ID, 입석예약수>
	 */
	Map<Long, Integer> countOverlappingStandingReservationsBatch(
		List<Long> trainScheduleIds,
		Long departureStationId,
		Long arrivalStationId
	);
}
