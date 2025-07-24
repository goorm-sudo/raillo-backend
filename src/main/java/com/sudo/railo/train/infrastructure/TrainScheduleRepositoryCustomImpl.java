package com.sudo.railo.train.infrastructure;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.train.application.dto.TrainBasicInfo;
import com.sudo.railo.train.domain.QScheduleStop;
import com.sudo.railo.train.domain.QStation;
import com.sudo.railo.train.domain.QTrain;
import com.sudo.railo.train.domain.QTrainCar;
import com.sudo.railo.train.domain.QTrainSchedule;
import com.sudo.railo.train.domain.status.OperationStatus;
import com.sudo.railo.train.domain.type.CarType;

import lombok.AllArgsConstructor;
import lombok.Getter;
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
	 * 열차 기본 정보 조회
	 * - 출발역, 도착역을 경유하는 모든 열차 조회
	 */
	@Override
	public Slice<TrainBasicInfo> findTrainBasicInfo(
		Long departureStationId, Long arrivalStationId, LocalDate operationDate,
		LocalTime departureTimeFrom, Pageable pageable) {

		QTrainSchedule ts = QTrainSchedule.trainSchedule;
		QTrain t = QTrain.train;
		QScheduleStop departureStop = new QScheduleStop("departureStop");
		QScheduleStop arrivalStop = new QScheduleStop("arrivalStop");
		QStation departureStation = new QStation("departureStation");
		QStation arrivalStation = new QStation("arrivalStation");

		// 1단계: 출발역과 도착역을 모두 경유하는 스케줄 ID 조회
		JPAQuery<Long> validScheduleSubQuery = queryFactory
			.select(ts.id)
			.from(ts)
			.join(departureStop).on(departureStop.trainSchedule.id.eq(ts.id))
			.join(arrivalStop).on(arrivalStop.trainSchedule.id.eq(ts.id))
			.where(
				ts.operationDate.eq(operationDate)
					.and(ts.operationStatus.eq(OperationStatus.ACTIVE))
					.and(departureStop.station.id.eq(departureStationId))
					.and(arrivalStop.station.id.eq(arrivalStationId))
					.and(departureStop.stopOrder.lt(arrivalStop.stopOrder))      // 정차 순서 : 출발역 < 도착역, less than
					.and(departureStop.departureTime.goe(departureTimeFrom))
				// 출발 시간 : 출발역 < 도착역, Greater than Or Equal
			)
			.groupBy(ts.id);

		// 2단계: 상세 정보 조회 (실제 출발/도착 시간, 역명)
		QScheduleStop depStop2 = new QScheduleStop("depStop2");
		QScheduleStop arrStop2 = new QScheduleStop("arrStop2");

		List<TrainBasicInfoWithStops> results = queryFactory
			.select(Projections.constructor(TrainBasicInfoWithStops.class,
				ts.id,
				t.trainNumber,
				t.trainName,
				depStop2.departureTime,
				arrStop2.arrivalTime,
				departureStation.stationName,
				arrivalStation.stationName))
			.from(ts)
			.join(ts.train, t)
			.join(depStop2).on(depStop2.trainSchedule.id.eq(ts.id))
			.join(depStop2.station, departureStation)
			.join(arrStop2).on(arrStop2.trainSchedule.id.eq(ts.id))
			.join(arrStop2.station, arrivalStation)
			.where(
				ts.id.in(validScheduleSubQuery)
					.and(depStop2.station.id.eq(departureStationId))
					.and(arrStop2.station.id.eq(arrivalStationId))
					.and(depStop2.stopOrder.lt(arrStop2.stopOrder))         // less than
			)
			.orderBy(depStop2.departureTime.asc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize() + 1)                             // hasNext 확인용 +1
			.fetch();

		// hasNext 확인 및 데이터 조정
		boolean hasNext = results.size() > pageable.getPageSize();
		List<TrainBasicInfoWithStops> content = hasNext ?
			results.subList(0, pageable.getPageSize()) : results;

		// DTO 변환
		List<TrainBasicInfo> trainBasicInfos = content.stream()
			.map(temp -> new TrainBasicInfo(
				temp.getScheduleId(),
				temp.getTrainNumber(),
				temp.getTrainName(),
				temp.getDepartureStationName(),
				temp.getArrivalStationName(),
				temp.getDepartureTime(),
				temp.getArrivalTime()
			))
			.collect(Collectors.toList());

		return new SliceImpl<>(trainBasicInfos, pageable, hasNext);
	}

	@Override
	public Map<Long, Map<CarType, Integer>> findTotalSeatsByCarTypeBatch(List<Long> trainScheduleIds) {

		if (trainScheduleIds.isEmpty()) {
			return Map.of();
		}

		QTrainSchedule trainSchedule = QTrainSchedule.trainSchedule;
		QTrain train = QTrain.train;
		QTrainCar trainCar = QTrainCar.trainCar;

		// Tuple[trainScheduleId=1, carType=STANDARD,    totalSeats=120]
		List<Tuple> results = queryFactory
			.select(
				trainSchedule.id,
				trainCar.carType,
				trainCar.totalSeats.sum()
			)
			.from(trainSchedule)
			.join(trainSchedule.train, train)
			.join(trainCar).on(trainCar.train.id.eq(train.id))
			.where(trainSchedule.id.in(trainScheduleIds))
			.groupBy(trainSchedule.id, trainCar.carType)
			.fetch();

		return results.stream()
			.collect(Collectors.groupingBy(
				tuple -> tuple.get(trainSchedule.id),                          // 1차 그룹핑: 열차별
				Collectors.toMap(                                              // 2차 Map 변환: 객차타입별 Map
					tuple -> tuple.get(trainCar.carType),                      // Key : 객차 타입
					tuple -> tuple.get(trainCar.totalSeats.sum()).intValue()   // Value : 좌석 수
				)
			));
	}

	/**
	 *  열차 최대 수용 인원 조회
	 */
	@Override
	public int findTotalSeatsByTrainScheduleId(Long trainScheduleId) {
		QTrainSchedule ts = QTrainSchedule.trainSchedule;
		QTrain t = QTrain.train;
		QTrainCar tc = QTrainCar.trainCar;

		// 해당 열차의 전체 좌석 수 계산
		Integer totalSeats = queryFactory
			.select(tc.totalSeats.sum())
			.from(ts)
			.join(ts.train, t)
			.join(tc).on(tc.train.eq(t))
			.where(ts.id.eq(trainScheduleId))
			.fetchOne();

		return totalSeats != null ? totalSeats : 0;
	}

	@Override
	public Map<Long, Integer> findTotalSeatsByTrainScheduleIdBatch(List<Long> trainScheduleIds) {

		if (trainScheduleIds.isEmpty()) {
			return Map.of();
		}

		QTrainSchedule trainSchedule = QTrainSchedule.trainSchedule;
		QTrain train = QTrain.train;
		QTrainCar trainCar = QTrainCar.trainCar;

		List<Tuple> results = queryFactory
			.select(
				trainSchedule.id,
				trainCar.totalSeats.sum()
			)
			.from(trainSchedule)
			.join(trainSchedule.train, train)
			.join(trainCar).on(trainCar.train.id.eq(train.id))
			.where(trainSchedule.id.in(trainScheduleIds))
			.groupBy(trainSchedule.id)
			.fetch();

		return results.stream()
			.collect(Collectors.toMap(
				tuple -> tuple.get(trainSchedule.id),
				tuple -> tuple.get(trainCar.totalSeats.sum()).intValue()
			));
	}

	@Getter
	@AllArgsConstructor
	public static class TrainBasicInfoWithStops {
		private Long scheduleId;
		private Integer trainNumber;
		private String trainName;
		private LocalTime departureTime;
		private LocalTime arrivalTime;
		private String departureStationName;
		private String arrivalStationName;
	}
}
