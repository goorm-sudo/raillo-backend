package com.sudo.railo.train.infrastructure;

public interface SeatReservationRepositoryCustom {

	/**
	 * 특정 구간에서 겹치는 입석(Standing) 예약 수 조회
	 */
	int countOverlappingStandingReservations(Long trainScheduleId, Long departureStationId, Long arrivalStationId);
}
