package com.sudo.railo.train.infrastructure;

import java.util.List;

import com.sudo.railo.train.application.dto.SeatReservationInfo;

public interface SeatReservationRepositoryCustom {

	/**
	 * 특정 구간과 겹치는 좌석 예약 조회
	 * - 요청한 출발역~도착역 구간과 겹치는 예약 찾기
	 *
	 * @param trainScheduleId 기차 스케줄 ID
	 * @param departureStationId 출발역 ID
	 * @param arrivalStationId 도착역 ID
	 * @return 겹치는 좌석 예약 정보 리스트
	 */
	List<SeatReservationInfo> findOverlappingReservations(
		Long trainScheduleId,
		Long departureStationId,
		Long arrivalStationId
	);

	/**
	 * 특정 구간에서 겹치는 입석(Standing) 예약 수 조회
	 */
	int countOverlappingStandingReservations(Long trainScheduleId, Long departureStationId, Long arrivalStationId);
}
