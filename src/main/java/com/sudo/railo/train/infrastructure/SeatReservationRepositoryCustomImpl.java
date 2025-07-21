package com.sudo.railo.train.infrastructure;

import static com.sudo.railo.booking.domain.QReservation.*;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
	 * 특정 구간과 겹치는 좌석 예약 조회
	 * - 요청한 출발역~도착역 구간과 겹치는 예약 찾기
	 */
	@Override
	public List<SeatReservationInfo> findOverlappingReservations(
		Long trainScheduleId, Long departureStationId, Long arrivalStationId
	) {
		QSeatReservation seatReservation = QSeatReservation.seatReservation;
		QSeat s = QSeat.seat;
		QTrainCar tc = QTrainCar.trainCar;
		QStation departureStation = new QStation("departureStation");
		QStation arrivalStation = new QStation("arrivalStation");

		/**
		 * 상행 / 하행 방향 판단 : 출발역 < 도착역이면 하행, 아니면 상행
		 * 역 ID가 낮은 쪽 → 높은 쪽: 하행 (예: 서울(10) → 부산(50))
		 * 역 ID가 높은 쪽 → 낮은 쪽: 상행 (예: 부산(50) → 서울(10))
		 */
		boolean isDownward = departureStationId < arrivalStationId;

		// 구간 겹침 조건 정의
		// 예약 구간: [기존출발 ~ 기존도착]
		// 요청 구간: [검색출발 ~ 검색도착]
		// 겹침 조건: 기존출발 < 검색도착 AND 기존도착 > 검색출발 (하행 기준)
		//           기존출발 > 검색도착 AND 기존도착 < 검색출발 (상행 기준)
		BooleanExpression overlapCondition = isDownward
			? departureStation.id.lt(arrivalStationId)
			.and(arrivalStation.id.gt(departureStationId)) // 하행
			: departureStation.id.gt(arrivalStationId)
			.and(arrivalStation.id.lt(departureStationId)); // 상행

		return queryFactory
			.select(Projections.constructor(
				SeatReservationInfo.class,
				seatReservation.seat.id,
				tc.carType,
				departureStation.id,
				arrivalStation.id
			))
			.from(seatReservation)
			.join(s).on(s.id.eq(seatReservation.seat.id))
			.join(tc).on(tc.id.eq(s.trainCar.id))
			.join(seatReservation.reservation, reservation)
			.join(reservation.departureStop.station, departureStation)
			.join(reservation.arrivalStop.station, arrivalStation)
			.where(
				seatReservation.trainSchedule.id.eq(trainScheduleId),
				seatReservation.seat.isNotNull(),
				overlapCondition  // 구간 겹침 조건
			)
			.fetch();
	}

	/**
	 * 특정 구간에서 겹치는 입석(Standing) 예약 수 조회
	 */
	@Override
	public int countOverlappingStandingReservations(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId) {
		QSeatReservation seatReservation = QSeatReservation.seatReservation;
		QScheduleStop searchDepartureStop = new QScheduleStop("searchDepartureStop");
		QScheduleStop searchArrivalStop = new QScheduleStop("searchArrivalStop");
		QScheduleStop reservedDepartureStop = new QScheduleStop("reservedDepartureStop");
		QScheduleStop reservedArrivalStop = new QScheduleStop("reservedArrivalStop");
		QStation reservedDepartureStation = new QStation("departureStation");
		QStation reservedArrivalStation = new QStation("arrivalStation");

		Long count = queryFactory.select(seatReservation.count())
			.from(seatReservation)
			.join(seatReservation.reservation, reservation)
			.join(reservation.departureStop, reservedDepartureStop)
			.join(reservation.arrivalStop, reservedArrivalStop)
			.join(reservedDepartureStop.station, reservedDepartureStation)
			.join(reservedArrivalStop.station, reservedArrivalStation)

			// TODO: 검증 필요 - 백업
			// 기존 예약의 출발역 정보 조회
			// .join(reservedDepartureStop)
			// .on(reservedDepartureStop.trainSchedule.id.eq(seatReservation.trainSchedule.id)
			// 	.and(reservedDepartureStop.station.id.eq(reservedDepartureStation.id)))
			// // 기존 예약의 도착역 정보 조회
			// .join(reservedArrivalStop)
			// .on(reservedArrivalStop.trainSchedule.id.eq(seatReservation.trainSchedule.id)
			// 	.and(reservedArrivalStop.station.id.eq(reservedArrivalStation.id)))

			// 검색 구간의 출발역 정보
			.join(searchDepartureStop)
			.on(searchDepartureStop.trainSchedule.id.eq(trainScheduleId)
				.and(searchDepartureStop.station.id.eq(departureStationId)))
			// 검색 구간의 도착역 정보
			.join(searchArrivalStop)
			.on(searchArrivalStop.trainSchedule.id.eq(trainScheduleId)
				.and(searchArrivalStop.station.id.eq(arrivalStationId)))
			.where(
				seatReservation.trainSchedule.id.eq(trainScheduleId),
				seatReservation.seat.isNull(),

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
		QScheduleStop departureStop = new QScheduleStop("departureStop");
		QScheduleStop arrivalStop = new QScheduleStop("arrivalStop");
		QStation departureStation = new QStation("departureStation");
		QStation arrivalStation = new QStation("arrivalStation");
		Long count = queryFactory.select(sr.count())
			.from(sr)
			.join(sr.reservation, reservation)
			.join(reservation.departureStop, departureStop)
			.join(reservation.arrivalStop, arrivalStop)
			.join(departureStop.station, departureStation)
			.join(arrivalStop.station, arrivalStation)
			.where(sr.trainSchedule.id.eq(trainScheduleId), sr.seat.id.eq(seatId),
				// 구간 겹침 확인
				departureStation.id.lt(arrivalStationId).and(arrivalStation.id.gt(departureStationId)))
			.fetchOne();

		return count == null || count == 0;
	}
}
