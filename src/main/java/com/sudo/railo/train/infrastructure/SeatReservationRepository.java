package com.sudo.railo.train.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sudo.railo.train.application.dto.SeatReservationInfo;
import com.sudo.railo.train.domain.SeatReservation;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

	/**
	 * 특정 구간과 겹치는 좌석 예약 조회
	 * - 요청한 출발역~도착역 구간과 겹치는 예약 찾기
	 * - 좌석 상태 계산의 핵심 로직
	 */
	// TODO : queryDSL로 변경
	@Query("""
		SELECT new com.sudo.railo.train.application.dto.SeatReservationInfo(
		    sr.seatId, tc.carType, sr.departureStationId, sr.arrivalStationId
		)
		FROM SeatReservation sr
		JOIN Seat s ON s.id = sr.seatId
		JOIN TrainCar tc ON tc.id = s.trainCar.id
		WHERE sr.trainScheduleId = :trainScheduleId
		AND sr.status IN ('RESERVED', 'PAID')
		AND NOT (sr.arrivalStationId <= :departureStationId OR sr.departureStationId >= :arrivalStationId)
		""")
	List<SeatReservationInfo> findOverlappingReservations(
		@Param("trainScheduleId") Long trainScheduleId,
		@Param("departureStationId") Long departureStationId,
		@Param("arrivalStationId") Long arrivalStationId);
}
