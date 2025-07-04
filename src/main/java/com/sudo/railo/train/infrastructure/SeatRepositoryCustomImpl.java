package com.sudo.railo.train.infrastructure;

import static com.sudo.railo.booking.domain.QSeatReservation.*;
import static com.sudo.railo.train.domain.QSeat.*;
import static com.sudo.railo.train.domain.QTrainCar.*;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.booking.domain.SeatStatus;
import com.sudo.railo.train.application.TrainCarSeatInfo;
import com.sudo.railo.train.application.dto.projection.QSeatProjection;
import com.sudo.railo.train.application.dto.projection.SeatProjection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SeatRepositoryCustomImpl implements SeatRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public TrainCarSeatInfo findTrainCarSeatDetail(
		Long trainCarId,
		Long trainScheduleId,
		Long departureStationId,
		Long arrivalStationId) {

		// 1. 객차 기본 정보 조회
		Tuple carInfo = queryFactory
			.select(
				trainCar.carNumber,
				trainCar.carType,
				trainCar.seatArrangement,
				trainCar.totalSeats
			)
			.from(trainCar)
			.where(trainCar.id.eq(trainCarId))
			.fetchOne();

		// 2. 좌석별 상세 정보 조회 (예약 상태 포함)
		List<SeatProjection> seatProjections = queryFactory
			.select(new QSeatProjection(
				seat.id,
				seat.seatRow.stringValue().concat(seat.seatColumn),
				seat.seatRow,
				seat.seatColumn,
				seat.seatType,
				// directionCode
				new CaseBuilder()
					.when(seat.seatRow.lt(8)) // 8보다 작으면
					.then("009")  // 순방향
					.otherwise("010"), // 역방향
				// 해당 구간에 예약이 있는지 확인
				new CaseBuilder()
					.when(seatReservation.id.isNotNull())
					.then(true)
					.otherwise(false),
				// 4인 동반석 안내 메시지
				new CaseBuilder()
					.when(seat.seatRow.between(8, 9))
					.then(new CaseBuilder()
						.when(seat.seatRow.eq(8))
						.then("KTX 4인동반석 순방향 좌석 입니다. 맞은편 좌석에 다른 승객이 승차할 수 있습니다.")
						.when(seat.seatRow.eq(9))
						.then("KTX 4인동반석 역방향 좌석 입니다. 맞은편 좌석에 다른 승객이 승차할 수 있습니다.")
						.otherwise(""))
					.otherwise("")
			))
			.from(seat)
			.leftJoin(seatReservation)
			.on(
				seatReservation.seat.id.eq(seat.id)
					.and(seatReservation.trainSchedule.id.eq(trainScheduleId))
					.and(seatReservation.seatStatus.in(SeatStatus.RESERVED, SeatStatus.LOCKED))
					.and(seatReservation.isStanding.isFalse())
					// 구간 겹침 확인 (기존출발 < 검색도착 AND 기존도착 > 검색출발)
					.and(seatReservation.departureStation.id.lt(arrivalStationId))
					.and(seatReservation.arrivalStation.id.gt(departureStationId))
			)
			.where(seat.trainCar.id.eq(trainCarId))
			.orderBy(seat.seatRow.asc(), seat.seatColumn.asc())
			.fetch();

		// 3. 잔여 좌석 수 계산
		long remainingSeats = seatProjections.stream()
			.mapToLong(projection -> projection.isAvailable() ? 1 : 0)
			.sum();

		return new TrainCarSeatInfo(
			String.valueOf(carInfo.get(trainCar.carNumber)),
			carInfo.get(trainCar.carType),
			carInfo.get(trainCar.seatArrangement),
			Optional.ofNullable(carInfo.get(trainCar.totalSeats)).orElse(0),
			(int)remainingSeats,
			seatProjections
		);
	}
}
