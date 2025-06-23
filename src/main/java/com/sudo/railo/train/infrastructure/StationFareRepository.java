package com.sudo.railo.train.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sudo.railo.train.domain.StationFare;

/**
 * 구간 별 요금 Repository
 * 출발역-도착역 구간의 요금 정보 조회
 */
@Repository
public interface StationFareRepository extends JpaRepository<StationFare, Long> {

	/**
	 * 출발역-도착역 구간의 요금 정보 조회
	 * - 일반실/특실 요금 정보
	 * - 열차 검색 결과에 요금 표시용
	 */
	Optional<StationFare> findByDepartureStationIdAndArrivalStationId(
		Long departureStationId, Long arrivalStationId);
}
