package com.sudo.railo.train.infrastructure;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sudo.railo.train.application.dto.TrainBasicInfo;
import com.sudo.railo.train.domain.type.CarType;

public interface TrainScheduleRepositoryCustom {

	/**
	 * 날짜 범위에서 활성 스케줄이 있는 날짜들 조회 (운행 스케줄 캘린더 조회)
	 * TODO : 성능 모니터링 필요 : operation_calender 테이블, 배치, 캐시로 성능 개선 예정
	 */
	Set<LocalDate> findDatesWithActiveSchedules(LocalDate startDate, LocalDate endDate);

	/**
	 * 열차 기본 정보 조회 (페이징) - 메인 검색 기능
	 * 출발역, 도착역, 운행날짜로 열차 목록 조회
	 * @param departureStationId 출발역 ID
	 * @param arrivalStationId 도착역 ID
	 * @param operationDate 운행 날짜
	 * @param departureTimeFrom 출발 희망 시간 이후
	 * @param pageable 페이징 정보
	 * @return 열차 기본 정보 페이지
	 */
	Page<TrainBasicInfo> findTrainBasicInfo(
		Long departureStationId,
		Long arrivalStationId,
		LocalDate operationDate,
		LocalTime departureTimeFrom,
		Pageable pageable
	);

	/**
	 * 열차의 좌석 타입별 전체 좌석 수 조회
	 * 좌석 상태 계산을 위한 기준 데이터
	 */
	Map<CarType, Integer> findTotalSeatsByCarType(Long trainScheduleId);

	/**
	 * 열차 최대 수용 인원 조회 (입석 포함)
	 * 입석 가능 여부 판단용
	 */
	int findMaxCapacityByTrainScheduleId(Long trainScheduleId);
}