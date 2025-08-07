package com.sudo.railo.train.infrastructure;

import static com.sudo.railo.booking.domain.QReservation.*;
import static com.sudo.railo.booking.domain.QSeatReservation.*;
import static com.sudo.railo.train.domain.QSeat.*;
import static com.sudo.railo.train.domain.QTrainCar.*;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.train.application.TrainCarSeatInfo;
import com.sudo.railo.train.application.dto.projection.QSeatProjection;
import com.sudo.railo.train.application.dto.projection.SeatProjection;
import com.sudo.railo.train.domain.QScheduleStop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SeatRepositoryCustomImpl implements SeatRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	/**
	 * 특정 객차의 모든 좌석 상세 정보 및 예약 상태 조회
	 * - LEFT JOIN으로 예약 정보 연결 (예약 없는 좌석도 포함)
	 * - stopOrder 기반으로 해당 구간에서의 예약 상태 판단
	 * - 좌석별 방향성, 특별 메시지 등 부가 정보 포함
	 */
	@Override
	public TrainCarSeatInfo findTrainCarSeatDetail(Long trainCarId, Long trainScheduleId, Long departureStationId,
		Long arrivalStationId) {

		// 1. 객차 기본 정보 조회
		Tuple carInfo = queryFactory.select(
				trainCar.carNumber,
				trainCar.carType,
				trainCar.seatArrangement,
				trainCar.totalSeats,
				trainCar.seatRowCount)
			.from(trainCar)
			.where(trainCar.id.eq(trainCarId))
			.fetchOne();

		// seatRowCount 로 middleRow 계산
		Integer seatRowCount = carInfo.get(trainCar.seatRowCount);
		int middleRow = (seatRowCount != null) ? (seatRowCount + 1) / 2 : 8; // 중간 지점 계산

		// 2. 객차 내 모든 좌석 상세 정보 조회 (예약 상태 포함)
		// stopOrder 기반 구간 겹침을 위한 ScheduleStop 조인
		QScheduleStop reservedDepartureStop = new QScheduleStop("reservedDepartureStop");
		QScheduleStop reservedArrivalStop = new QScheduleStop("reservedArrivalStop");
		QScheduleStop searchDepartureStop = new QScheduleStop("searchDepartureStop");
		QScheduleStop searchArrivalStop = new QScheduleStop("searchArrivalStop");

		List<SeatProjection> seatProjections = queryFactory.select(
				new QSeatProjection(seat.id,
					seat.seatRow.stringValue().concat(seat.seatColumn),
					seat.seatType,
					// directionCode
					new CaseBuilder().when(seat.seatRow.loe(middleRow)) // 중간 이하 : 순방향
						.then("009")  // 순방향
						.otherwise("010"), // 역방향
					// isReserved
					new CaseBuilder().when(seatReservation.id.isNotNull()).then(true).otherwise(false),
					// specialMessage
					new CaseBuilder().when(seat.seatRow.between(middleRow, middleRow + 1))
						.then(new CaseBuilder().when(seat.seatRow.eq(middleRow))
							.then("KTX 4인동반석 순방향 좌석 입니다. 맞은편 좌석에 다른 승객이 승차할 수 있습니다.")
							.when(seat.seatRow.eq(middleRow + 1))
							.then("KTX 4인동반석 역방향 좌석 입니다. 맞은편 좌석에 다른 승객이 승차할 수 있습니다.")
							.otherwise(""))
						.otherwise("")))
			.from(seat)
			.leftJoin(seatReservation).on(
				seatReservation.seat.id.eq(seat.id)
					.and(seatReservation.trainSchedule.id.eq(trainScheduleId))
					.and(seatReservation.seat.isNotNull())  // 실제 좌석 예약 (입석 X)
			)
			.leftJoin(seatReservation.reservation, reservation)
			// 기존 예약 정보 left join
			.leftJoin(reservation.departureStop, reservedDepartureStop)
			.leftJoin(reservation.arrivalStop, reservedArrivalStop)
			// 검색 구간 정보 left join
			.leftJoin(searchDepartureStop).on(
				searchDepartureStop.trainSchedule.id.eq(trainScheduleId)
					.and(searchDepartureStop.station.id.eq(departureStationId))
			)
			.leftJoin(searchArrivalStop).on(
				searchArrivalStop.trainSchedule.id.eq(trainScheduleId)
					.and(searchArrivalStop.station.id.eq(arrivalStationId))
			)
			.where(
				seat.trainCar.id.eq(trainCarId),
				// stopOrder 기반 구간 겹침 조건
				// 구간 겹침: NOT(예약종료 <= 검색시작 OR 예약시작 >= 검색종료)
				// = 예약종료 > 검색시작 AND 예약시작 < 검색종료
				reservation.id.isNull().or(
					reservedArrivalStop.stopOrder.gt(searchDepartureStop.stopOrder)
						.and(reservedDepartureStop.stopOrder.lt(searchArrivalStop.stopOrder))
				)
			)
			.orderBy(seat.seatRow.asc(), seat.seatColumn.asc())
			.fetch();

		// 3. 잔여 좌석 수 계산
		long remainingSeats = seatProjections.stream()
			.mapToLong(projection -> projection.isAvailable() ? 1 : 0)
			.sum();

		return new TrainCarSeatInfo(String.valueOf(carInfo.get(trainCar.carNumber)), carInfo.get(trainCar.carType),
			carInfo.get(trainCar.seatArrangement), Optional.ofNullable(carInfo.get(trainCar.totalSeats)).orElse(0),
			(int)remainingSeats, seatProjections);
	}
}
