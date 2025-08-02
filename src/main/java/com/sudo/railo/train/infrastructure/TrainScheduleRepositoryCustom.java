package com.sudo.railo.train.infrastructure;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import com.sudo.railo.train.application.dto.TrainBasicInfo;
import com.sudo.railo.train.application.dto.projection.TrainSeatInfoBatch;

public interface TrainScheduleRepositoryCustom {

	/**
	 * 날짜 범위에서 활성 스케줄이 있는 날짜들 조회 (운행 스케줄 캘린더 조회)
	 * TODO : 성능 모니터링 필요 : operation_calendar 테이블, 배치, 캐시로 성능 개선 예정
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
	Slice<TrainBasicInfo> findTrainBasicInfo(
		Long departureStationId,
		Long arrivalStationId,
		LocalDate operationDate,
		LocalTime departureTimeFrom,
		Pageable pageable
	);

	/**
	 * 여러 열차의 객차 타입별, 열차 전체 인원 조회
	 * @param trainScheduleIds
	 * @return TrainSeatInfoBatch (Map<열차스케줄ID, Map < 객차타입, 좌석수>>, Map<열차스케줄ID, 전체좌석수>)
	 */
	TrainSeatInfoBatch findTrainSeatInfoBatch(List<Long> trainScheduleIds);
}
