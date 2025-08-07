package com.sudo.railo.train.infrastructure;

import static com.sudo.railo.booking.domain.QReservation.*;
import static com.sudo.railo.booking.domain.QSeatReservation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.booking.application.dto.projection.QSeatInfoProjection;
import com.sudo.railo.booking.application.dto.projection.SeatInfoProjection;
import com.sudo.railo.booking.domain.QSeatReservation;
import com.sudo.railo.train.application.dto.SeatReservationInfo;
import com.sudo.railo.train.domain.QScheduleStop;
import com.sudo.railo.train.domain.QSeat;
import com.sudo.railo.train.domain.QStation;
import com.sudo.railo.train.domain.QTrainCar;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SeatReservationRepositoryCustomImpl implements SeatReservationRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	/**
	 * 여러 열차의 특정 구간에서 겹치는 예약 정보를 일괄 조회
	 * - 요청한 출발역~도착역 구간과 겹치는 예약 찾기
	 */
	@Override
	public Map<Long, List<SeatReservationInfo>> findOverlappingReservationsBatch(List<Long> trainScheduleIds,
		Long departureStationId, Long arrivalStationId) {

		if (trainScheduleIds.isEmpty()) {
			return Map.of();
		}

		QSeatReservation seatReservation = QSeatReservation.seatReservation;
		QSeat seat = QSeat.seat;
		QTrainCar trainCar = QTrainCar.trainCar;
		QScheduleStop reservedDepartureStop = new QScheduleStop("reservedDepartureStop");
		QScheduleStop reservedArrivalStop = new QScheduleStop("reservedArrivalStop");
		QStation reservedDepartureStation = new QStation("reservedDepartureStation");
		QStation reservedArrivalStation = new QStation("reservedArrivalStation");
		QScheduleStop searchDepartureStop = new QScheduleStop("searchDepartureStop");
		QScheduleStop searchArrivalStop = new QScheduleStop("searchArrivalStop");

		List<Tuple> results = queryFactory
			.select(
				seatReservation.trainSchedule.id,
				seatReservation.seat.id,
				trainCar.carType,
				reservedDepartureStation.id,
				reservedArrivalStation.id
			)
			.from(seatReservation)
			.join(seat).on(seat.id.eq(seatReservation.seat.id))            // 좌석 정보
			.join(trainCar).on(trainCar.id.eq(seat.trainCar.id))           // 객차 정보 (객차 타입 판별 : 일반실/특실)
			.join(seatReservation.reservation, reservation)                // 예약 정보 (seatReservation 에만 좌석 정보 존재)
			.join(reservation.departureStop, reservedDepartureStop)           // 출발역
			.join(reservation.arrivalStop, reservedArrivalStop)               // 도착역
			.join(reservedDepartureStop.station, reservedDepartureStation)
			.join(reservedArrivalStop.station, reservedArrivalStation)
			.join(searchDepartureStop).on(
				searchDepartureStop.trainSchedule.id.in(trainScheduleIds)
					.and(searchDepartureStop.station.id.eq(departureStationId))
			)
			.join(searchArrivalStop).on(
				searchArrivalStop.trainSchedule.id.eq(seatReservation.trainSchedule.id)
					.and(searchArrivalStop.station.id.eq(arrivalStationId))
			)
			.where(
				seatReservation.trainSchedule.id.in(trainScheduleIds),             // 해당 trainScheduleId 모두 조회
				seatReservation.seat.isNotNull(),                                     // 실제 좌석 예약(입석 X)
				reservedArrivalStop.stopOrder.gt(searchDepartureStop.stopOrder)         // 구간 겹침 조건
					.and(reservedDepartureStop.stopOrder.lt(searchArrivalStop.stopOrder))
			)
			.fetch();

		// 결과를 trainScheduleId별로 그룹핑
		return results.stream()
			.collect(Collectors.groupingBy(
				tuple -> tuple.get(seatReservation.trainSchedule.id),
				Collectors.mapping(tuple -> new SeatReservationInfo(
					tuple.get(seatReservation.seat.id),
					tuple.get(trainCar.carType),
					tuple.get(reservedDepartureStation.id),
					tuple.get(reservedArrivalStation.id)
				), Collectors.toList())
			));
	}

	/**
	 * 여러 열차의 특정 구간에서 겹치는 입석 예약 수를 일괄 조회
	 */
	@Override
	public Map<Long, Integer> countOverlappingStandingReservationsBatch(List<Long> trainScheduleIds,
		Long departureStationId, Long arrivalStationId) {

		if (trainScheduleIds.isEmpty()) {
			return Map.of();
		}

		QSeatReservation seatReservation = QSeatReservation.seatReservation;
		QScheduleStop reservedDepartureStop = new QScheduleStop("reservedDepartureStop");
		QScheduleStop reservedArrivalStop = new QScheduleStop("reservedArrivalStop");
		QScheduleStop searchDepartureStop = new QScheduleStop("searchDepartureStop");
		QScheduleStop searchArrivalStop = new QScheduleStop("searchArrivalStop");

		List<Tuple> results = queryFactory
			.select(
				seatReservation.trainSchedule.id,
				seatReservation.count()
			)
			.from(seatReservation)
			.join(seatReservation.reservation, reservation)
			.join(reservation.departureStop, reservedDepartureStop)
			.join(reservation.arrivalStop, reservedArrivalStop)
			.join(searchDepartureStop).on(
				searchDepartureStop.trainSchedule.id.in(trainScheduleIds)
					.and(searchDepartureStop.station.id.eq(departureStationId))
			)
			.join(searchArrivalStop).on(
				searchArrivalStop.trainSchedule.id.eq(seatReservation.trainSchedule.id)
					.and(searchArrivalStop.station.id.eq(arrivalStationId))
			)
			.where(
				seatReservation.trainSchedule.id.in(trainScheduleIds),
				seatReservation.seat.isNull(),                                // 입석만
				reservedArrivalStop.stopOrder.gt(searchDepartureStop.stopOrder)
					.and(reservedDepartureStop.stopOrder.lt(searchArrivalStop.stopOrder))
			)
			.groupBy(seatReservation.trainSchedule.id)
			.fetch();

		return results.stream()
			.collect(Collectors.toMap(
				tuple -> tuple.get(seatReservation.trainSchedule.id),
				tuple -> tuple.get(seatReservation.count()).intValue()
			));
	}

	/**
	 * 예약 ID로 해당 예약의 좌석 정보와 승객 타입을 조회 (PaymentService용)
	 */
	@Override
	public List<SeatInfoProjection> findSeatInfoByReservationId(Long reservationId) {
		return queryFactory
			.select(new QSeatInfoProjection(
				seatReservation.seat,
				seatReservation.passengerType
			))
			.from(seatReservation)
			.where(seatReservation.reservation.id.eq(reservationId))
			.fetch();
	}
}
