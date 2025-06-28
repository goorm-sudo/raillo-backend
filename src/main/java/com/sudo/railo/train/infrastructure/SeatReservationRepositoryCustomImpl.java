package com.sudo.railo.train.infrastructure;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.booking.domain.ReservationStatus;
import com.sudo.railo.train.application.dto.SeatReservationInfo;
import com.sudo.railo.train.domain.QScheduleStop;
import com.sudo.railo.train.domain.QSeat;
import com.sudo.railo.train.domain.QSeatReservation;
import com.sudo.railo.train.domain.QTrainCar;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SeatReservationRepositoryCustomImpl implements SeatReservationRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	/**
	 * 특정 구간과 겹치는 좌석 예약 조회
	 * - 요청한 출발역~도착역 구간과 겹치는 예약 찾기
	 */
	@Override
	public List<SeatReservationInfo> findOverlappingReservations(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId) {
		QSeatReservation sr = QSeatReservation.seatReservation;
		QSeat s = QSeat.seat;
		QTrainCar tc = QTrainCar.trainCar;

		return queryFactory
			.select(Projections.constructor(
				SeatReservationInfo.class,
				sr.seatId,
				tc.carType,
				sr.departureStationId,
				sr.arrivalStationId
			))
			.from(sr)
			.join(s).on(s.id.eq(sr.seatId))
			.join(tc).on(tc.id.eq(s.trainCar.id))
			.where(
				sr.trainScheduleId.eq(trainScheduleId),
				sr.status.in(ReservationStatus.RESERVED, ReservationStatus.PAID),

				// 기존출발 < 검색도착 AND 기존도착 > 검색출발
				sr.departureStationId.lt(arrivalStationId)               // 예약출발 < 검색도착 (less than)
					.and(sr.arrivalStationId.gt(departureStationId))   // 예약도착 > 검색출발 (greater than)
			)
			.fetch();
	}

	/**
	 * 특정 구간에서 겹치는 입석(Standing) 예약 수 조회
	 */
	@Override
	public int countOverlappingStandingReservations(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId) {
		QSeatReservation reservation = QSeatReservation.seatReservation;
		QScheduleStop searchDepartureStop = new QScheduleStop("searchDepartureStop");
		QScheduleStop searchArrivalStop = new QScheduleStop("searchArrivalStop");
		QScheduleStop reservedDepartureStop = new QScheduleStop("reservedDepartureStop");
		QScheduleStop reservedArrivalStop = new QScheduleStop("reservedArrivalStop");

		Long count = queryFactory
			.select(reservation.count())
			.from(reservation)
			// 기존 예약의 출발역 정보 조회
			.join(reservedDepartureStop)
			.on(reservedDepartureStop.trainSchedule.id.eq(reservation.trainScheduleId)
				.and(reservedDepartureStop.station.id.eq(reservation.departureStationId)))
			// 기존 예약의 도착역 정보 조회
			.join(reservedArrivalStop)
			.on(reservedArrivalStop.trainSchedule.id.eq(reservation.trainScheduleId)
				.and(reservedArrivalStop.station.id.eq(reservation.arrivalStationId)))
			// 검색 구간의 출발역 정보
			.join(searchDepartureStop)
			.on(searchDepartureStop.trainSchedule.id.eq(trainScheduleId)
				.and(searchDepartureStop.station.id.eq(departureStationId)))
			// 검색 구간의 도착역 정보
			.join(searchArrivalStop)
			.on(searchArrivalStop.trainSchedule.id.eq(trainScheduleId)
				.and(searchArrivalStop.station.id.eq(arrivalStationId)))
			.where(
				reservation.trainScheduleId.eq(trainScheduleId),
				reservation.status.in(ReservationStatus.RESERVED, ReservationStatus.PAID),
				reservation.isStanding.isTrue(),

				// 구간 겹침 조건 (Interval Overlap Algorithm)
				// NOT(end1 <= start2 OR start1 >= end2) = NOT(안겹침)
				// 기존출발 < 검색도착 AND 기존도착 > 검색출발
				reservedDepartureStop.stopOrder.lt(searchArrivalStop.stopOrder) // less than
					.and(reservedArrivalStop.stopOrder.gt(searchDepartureStop.stopOrder)) // greater than
			)
			.fetchOne();

		return count != null ? count.intValue() : 0;
	}
}
