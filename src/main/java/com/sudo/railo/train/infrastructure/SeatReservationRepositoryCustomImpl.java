package com.sudo.railo.train.infrastructure;

import static com.sudo.railo.booking.domain.QReservation.*;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.booking.domain.QSeatReservation;
import com.sudo.railo.global.util.TrackQuery;
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
	 * 특정 열차 구간과 겹치는 좌석 예약 조회
	 * - 요청한 출발역~도착역 구간과 겹치는 예약 찾기
	 */
	@TrackQuery(queryName = "findOverlappingReservations")
	@Override
	public List<SeatReservationInfo> findOverlappingReservations(
		Long trainScheduleId, Long departureStationId, Long arrivalStationId
	) {
		QSeatReservation seatReservation = QSeatReservation.seatReservation;
		QSeat seat = QSeat.seat;
		QTrainCar trainCar = QTrainCar.trainCar;

		// 예약된 구간의 정차역 정보
		QScheduleStop reservedDepartureStop = new QScheduleStop("reservedDepartureStop");
		QScheduleStop reservedArrivalStop = new QScheduleStop("reservedArrivalStop");
		QStation reservedDepartureStation = new QStation("reservedDepartureStation");
		QStation reservedArrivalStation = new QStation("reservedArrivalStation");

		// 검색 구간의 정차역 정보
		QScheduleStop searchDepartureStop = new QScheduleStop("searchDepartureStop");
		QScheduleStop searchArrivalStop = new QScheduleStop("searchArrivalStop");

		return queryFactory
			.select(Projections.constructor(
				SeatReservationInfo.class,
				seatReservation.seat.id,
				trainCar.carType,
				reservedDepartureStation.id,
				reservedArrivalStation.id
			))
			.from(seatReservation)
			// 좌석 및 객차 정보 조인 (예약된 좌석)
			.join(seat).on(seat.id.eq(seatReservation.seat.id))
			.join(trainCar).on(trainCar.id.eq(seat.trainCar.id))
			.join(seatReservation.reservation, reservation)

			// 기존 예약의 구간 정보 조인
			.join(reservation.departureStop, reservedDepartureStop)
			.join(reservation.arrivalStop, reservedArrivalStop)
			.join(reservedDepartureStop.station, reservedDepartureStation)
			.join(reservedArrivalStop.station, reservedArrivalStation)

			// 검색 구간 정보 조인
			.join(searchDepartureStop).on(
				searchDepartureStop.trainSchedule.id.eq(trainScheduleId)
					.and(searchDepartureStop.station.id.eq(departureStationId))
			)
			.join(searchArrivalStop).on(
				searchArrivalStop.trainSchedule.id.eq(trainScheduleId)
					.and(searchArrivalStop.station.id.eq(arrivalStationId))
			)
			.where(
				seatReservation.trainSchedule.id.eq(trainScheduleId),
				seatReservation.seat.isNotNull(),

				// stopOrder 기반 구간 겹침 조건
				// 구간 겹침: NOT(예약종료 <= 검색시작 OR 예약시작 >= 검색종료)
				// = 예약종료 > 검색시작 AND 예약시작 < 검색종료
				reservedArrivalStop.stopOrder.gt(searchDepartureStop.stopOrder)
					.and(reservedDepartureStop.stopOrder.lt(searchArrivalStop.stopOrder))
			)
			.fetch();
	}

	/**
	 * 특정 구간에서 겹치는 입석(Standing) 예약 개수 조회
	 */
	@TrackQuery(queryName = "countOverlappingStandingReservations")
	@Override
	public int countOverlappingStandingReservations(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId) {
		QSeatReservation seatReservation = QSeatReservation.seatReservation;

		// 예약된 구간의 정차역 정보
		QScheduleStop reservedDepartureStop = new QScheduleStop("reservedDepartureStop");
		QScheduleStop reservedArrivalStop = new QScheduleStop("reservedArrivalStop");
		QStation reservedDepartureStation = new QStation("reservedDepartureStation");
		QStation reservedArrivalStation = new QStation("reservedArrivalStation");

		// 검색 구간의 정차역 정보
		QScheduleStop searchDepartureStop = new QScheduleStop("searchDepartureStop");
		QScheduleStop searchArrivalStop = new QScheduleStop("searchArrivalStop");

		Long count = queryFactory
			.select(seatReservation.count())
			.from(seatReservation)
			.join(seatReservation.reservation, reservation)

			// 기존 예약의 구간 정보 조인
			.join(reservation.departureStop, reservedDepartureStop)
			.join(reservation.arrivalStop, reservedArrivalStop)
			.join(reservedDepartureStop.station, reservedDepartureStation)
			.join(reservedArrivalStop.station, reservedArrivalStation)

			// 검색 구간 정보 조인
			.join(searchDepartureStop).on(
				searchDepartureStop.trainSchedule.id.eq(trainScheduleId)
					.and(searchDepartureStop.station.id.eq(departureStationId))
			)
			.join(searchArrivalStop).on(
				searchArrivalStop.trainSchedule.id.eq(trainScheduleId)
					.and(searchArrivalStop.station.id.eq(arrivalStationId))
			)
			.where(
				seatReservation.trainSchedule.id.eq(trainScheduleId),
				seatReservation.seat.isNull(), // 입석 예약만

				// stopOrder 기반 구간 겹침 조건
				// 구간 겹침: NOT(예약종료 <= 검색시작 OR 예약시작 >= 검색종료)
				// = 예약종료 > 검색시작 AND 예약시작 < 검색종료
				reservedArrivalStop.stopOrder.gt(searchDepartureStop.stopOrder)
					.and(reservedDepartureStop.stopOrder.lt(searchArrivalStop.stopOrder))
			)
			.fetchOne();

		return count != null ? count.intValue() : 0;
	}

	/**
	 * 특정 좌석의 특정 구간 예약 충돌 여부 확인
	 * - 해당 구간에서 좌석이 이미 점유되어 있는지 확인
	 */
	@TrackQuery(queryName = "isSeatAvailableForSection")
	@Override
	public boolean isSeatAvailableForSection(Long trainScheduleId, Long seatId, Long departureStationId,
		Long arrivalStationId) {
		QSeatReservation seatReservation = QSeatReservation.seatReservation;

		// stopOrder 기반 구간 겹침을 위한 ScheduleStop 조인
		QScheduleStop reservedDepartureStop = new QScheduleStop("reservedDepartureStop");
		QScheduleStop reservedArrivalStop = new QScheduleStop("reservedArrivalStop");
		QScheduleStop searchDepartureStop = new QScheduleStop("searchDepartureStop");
		QScheduleStop searchArrivalStop = new QScheduleStop("searchArrivalStop");

		Long count = queryFactory
			.select(seatReservation.count())
			.from(seatReservation)
			.join(seatReservation.reservation, reservation)
			// 기존 예약의 구간 정보 조인
			.join(reservation.departureStop, reservedDepartureStop)
			.join(reservation.arrivalStop, reservedArrivalStop)

			// 검색 구간 정보 조인
			.join(searchDepartureStop).on(
				searchDepartureStop.trainSchedule.id.eq(trainScheduleId)
					.and(searchDepartureStop.station.id.eq(departureStationId))
			)
			.join(searchArrivalStop).on(
				searchArrivalStop.trainSchedule.id.eq(trainScheduleId)
					.and(searchArrivalStop.station.id.eq(arrivalStationId))
			)
			.where(
				seatReservation.trainSchedule.id.eq(trainScheduleId),
				seatReservation.seat.id.eq(seatId),

				// stopOrder 기반 구간 겹침 조건
				// 구간 겹침: NOT(예약종료 <= 검색시작 OR 예약시작 >= 검색종료)
				// = 예약종료 > 검색시작 AND 예약시작 < 검색종료
				reservedArrivalStop.stopOrder.gt(searchDepartureStop.stopOrder)
					.and(reservedDepartureStop.stopOrder.lt(searchArrivalStop.stopOrder))
			)
			.fetchOne();

		return count == null || count == 0;
	}
}
