package com.sudo.railo.train.infrastructure;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.train.application.dto.TrainBasicInfo;
import com.sudo.railo.train.domain.QTrain;
import com.sudo.railo.train.domain.QTrainCar;
import com.sudo.railo.train.domain.QTrainSchedule;
import com.sudo.railo.train.domain.status.OperationStatus;
import com.sudo.railo.train.domain.type.CarType;

import lombok.RequiredArgsConstructor;

/**
 * 열차 스케줄 커스텀 Repository 구현체
 * QueryDSL을 활용한 복잡한 쿼리 및 성능이 중요한 복잡한 조회 로직
 */
@Repository
@RequiredArgsConstructor
public class TrainScheduleRepositoryCustomImpl implements TrainScheduleRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	/**
	 * 날짜 범위에서 활성 스케줄이 있는 날짜들 조회 (운행 스케줄 캘린더 조회)
	 * TODO : 성능 모니터링 필요 : operation_calender 테이블, 배치, 캐시로 성능 개선 예정
	 */
	@Override
	public Set<LocalDate> findDatesWithActiveSchedules(LocalDate startDate, LocalDate endDate) {
		QTrainSchedule trainSchedule = QTrainSchedule.trainSchedule;

		List<LocalDate> dates = queryFactory
			.select(trainSchedule.operationDate)
			.distinct()
			.from(trainSchedule)
			.where(
				trainSchedule.operationDate.between(startDate, endDate)
					.and(trainSchedule.operationStatus.in(OperationStatus.ACTIVE, OperationStatus.DELAYED))
			)
			.orderBy(trainSchedule.operationDate.asc())
			.fetch();

		return new LinkedHashSet<>(dates);
	}

	/**
	 *  열차 기본 정보 조회 (페이징)
	 * - 출발역, 도착역, 운행날짜 조건으로 열차 목록 조회
	 * - 출발시간 순으로 정렬
	 * - 페이징 처리
	 */
	@Override
	public Page<TrainBasicInfo> findTrainBasicInfo(
		Long departureStationId, Long arrivalStationId, LocalDate operationDate, Pageable pageable) {

		QTrainSchedule ts = QTrainSchedule.trainSchedule;
		QTrain t = QTrain.train;

		// 열차 기본 정보 조회 쿼리
		List<TrainBasicInfo> content = queryFactory
			.select(Projections.constructor(TrainBasicInfo.class,
				ts.id,                              // 열차 스케줄 ID
				t.trainNumber,                    // 열차 번호
				t.trainName,                        // 열차명 (KTX, SRT 등)
				ts.departureTime,                   // 출발 시간
				ts.arrivalTime))                    // 도착 시간
			.from(ts)
			.join(ts.train, t)                      // 열차 정보 조인
			.where(
				ts.departureStation.id.eq(departureStationId),     // 출발역 조건
				ts.arrivalStation.id.eq(arrivalStationId),         // 도착역 조건
				ts.operationDate.eq(operationDate),                // 운행날짜 조건
				ts.operationStatus.eq(OperationStatus.ACTIVE)      // 운행 중인 열차만
			)
			.orderBy(ts.departureTime.asc())        // 출발시간 오름차순 정렬
			.offset(pageable.getOffset())           // 페이징 시작점
			.limit(pageable.getPageSize())          // 페이징 크기
			.fetch();

		// 전체 개수 조회 (페이징 정보용)
		Long total = queryFactory
			.select(ts.count())
			.from(ts)
			.where(
				ts.departureStation.id.eq(departureStationId),
				ts.arrivalStation.id.eq(arrivalStationId),
				ts.operationDate.eq(operationDate),
				ts.operationStatus.eq(OperationStatus.ACTIVE)
			)
			.fetchOne();

		return new PageImpl<>(content, pageable, total != null ? total : 0);
	}

	/**
	 *  열차의 좌석 타입별 전체 좌석 수 조회
	 * - 일반실/특실별 총 좌석 수 계산
	 * - 좌석 상태 계산의 기준 데이터
	 */
	@Override
	public Map<CarType, Integer> findTotalSeatsByCarType(Long trainScheduleId) {
		QTrainSchedule ts = QTrainSchedule.trainSchedule;
		QTrain t = QTrain.train;
		QTrainCar tc = QTrainCar.trainCar;

		// 객차별 좌석 수를 타입별로 합계 계산
		List<Tuple> results = queryFactory
			.select(tc.carType, tc.totalSeats.sum())    // 객차타입별 좌석수 합계
			.from(ts)
			.join(ts.train, t)                          // 열차 조인
			.join(t.trainCars, tc)                      // 객차 조인
			.where(ts.id.eq(trainScheduleId))           // 특정 열차 스케줄
			.groupBy(tc.carType)                        // 객차 타입별 그룹화
			.fetch();

		// Map으로 변환: {STANDARD=246, FIRST_CLASS=117}
		return results.stream().collect(Collectors.toMap(
			tuple -> tuple.get(tc.carType),                    // Key: 객차타입
			tuple -> tuple.get(tc.totalSeats.sum()).intValue() // Value: 좌석수
		));
	}

	/**
	 *  열차 최대 수용 인원 조회 (입석 포함)
	 * - 전체 좌석 수 + 입석 20% 추가 수용
	 * - 입석 가능 여부 판단에 사용
	 */
	@Override
	public int findMaxCapacityByTrainScheduleId(Long trainScheduleId) {
		QTrainSchedule ts = QTrainSchedule.trainSchedule;
		QTrain t = QTrain.train;
		QTrainCar tc = QTrainCar.trainCar;

		// 해당 열차의 전체 좌석 수 계산
		Integer totalSeats = queryFactory
			.select(tc.totalSeats.sum())
			.from(ts)
			.join(ts.train, t)
			.join(t.trainCars, tc)
			.where(ts.id.eq(trainScheduleId))
			.fetchOne();

		// 입석 20% 추가 수용 (KTX 정책)
		return totalSeats != null ? (int)(totalSeats * 1.2) : 0;
	}
}