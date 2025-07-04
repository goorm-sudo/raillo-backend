package com.sudo.railo.train.infrastructure;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.booking.domain.QSeatReservation;
import com.sudo.railo.booking.domain.SeatStatus;
import com.sudo.railo.train.application.dto.SeatReservationInfo;
import com.sudo.railo.train.domain.QScheduleStop;
import com.sudo.railo.train.domain.QSeat;
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
		QSeatReservation reservation = QSeatReservation.seatReservation;
		QSeat s = QSeat.seat;
		QTrainCar tc = QTrainCar.trainCar;

		return queryFactory
			.select(Projections.constructor(
				SeatReservationInfo.class,
				reservation.seat.id,
				tc.carType,
				reservation.departureStation.id,
				reservation.arrivalStation.id
			))
			.from(reservation)
			.join(s).on(s.id.eq(reservation.seat.id))
			.join(tc).on(tc.id.eq(s.trainCar.id))
			.where(
				reservation.trainSchedule.id.eq(trainScheduleId),
				reservation.seatStatus.in(SeatStatus.RESERVED, SeatStatus.LOCKED),
				reservation.isStanding.isFalse(),

				// 기존출발 < 검색도착 AND 기존도착 > 검색출발
				reservation.departureStation.id.lt(arrivalStationId)               // 예약출발 < 검색도착 (less than)
					.and(reservation.arrivalStation.id.gt(departureStationId))   // 예약도착 > 검색출발 (greater than)
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

		Long count = queryFactory.select(reservation.count())
			.from(reservation)
			// 기존 예약의 출발역 정보 조회
			.join(reservedDepartureStop)
			.on(reservedDepartureStop.trainSchedule.id.eq(reservation.trainSchedule.id)
				.and(reservedDepartureStop.station.id.eq(reservation.departureStation.id)))
			// 기존 예약의 도착역 정보 조회
			.join(reservedArrivalStop)
			.on(reservedArrivalStop.trainSchedule.id.eq(reservation.trainSchedule.id)
				.and(reservedArrivalStop.station.id.eq(reservation.arrivalStation.id)))
			// 검색 구간의 출발역 정보
			.join(searchDepartureStop)
			.on(searchDepartureStop.trainSchedule.id.eq(trainScheduleId)
				.and(searchDepartureStop.station.id.eq(departureStationId)))
			// 검색 구간의 도착역 정보
			.join(searchArrivalStop)
			.on(searchArrivalStop.trainSchedule.id.eq(trainScheduleId)
				.and(searchArrivalStop.station.id.eq(arrivalStationId)))
			.where(
				reservation.trainSchedule.id.eq(trainScheduleId),
				reservation.seatStatus.in(SeatStatus.RESERVED, SeatStatus.LOCKED),
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

	/**
	 * 특정 좌석의 예약 가능 여부 확인
	 * - 해당 구간에서 좌석이 이미 점유되어 있는지 확인
	 */
	@Override
	public boolean isSeatAvailableForSection(Long trainScheduleId, Long seatId, Long departureStationId,
		Long arrivalStationId) {
		QSeatReservation sr = QSeatReservation.seatReservation;

		Long count = queryFactory.select(sr.count())
			.from(sr)
			.where(sr.trainSchedule.id.eq(trainScheduleId), sr.seat.id.eq(seatId),
				// 점유 상태인 예약들만 확인
				sr.seatStatus.in(SeatStatus.RESERVED, SeatStatus.LOCKED), sr.isStanding.isFalse(),

				// 구간 겹침 확인
				sr.departureStation.id.lt(arrivalStationId).and(sr.arrivalStation.id.gt(departureStationId)))
			.fetchOne();

		return count == null || count == 0;
	}
}
