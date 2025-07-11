package com.sudo.railo.booking.infra;

import static com.sudo.railo.booking.domain.QReservation.*;
import static com.sudo.railo.booking.domain.QSeatReservation.*;
import static com.sudo.railo.train.domain.QSeat.*;
import static com.sudo.railo.train.domain.QStationFare.*;
import static com.sudo.railo.train.domain.QTrain.*;
import static com.sudo.railo.train.domain.QTrainCar.*;
import static com.sudo.railo.train.domain.QTrainSchedule.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.booking.application.dto.ReservationInfo;
import com.sudo.railo.booking.application.dto.projection.QReservationProjection;
import com.sudo.railo.booking.application.dto.projection.QSeatReservationProjection;
import com.sudo.railo.booking.application.dto.projection.ReservationProjection;
import com.sudo.railo.booking.application.dto.projection.SeatReservationProjection;
import com.sudo.railo.train.domain.QStation;
import com.sudo.railo.train.domain.type.CarType;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryCustomImpl implements ReservationRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<ReservationInfo> findReservationDetail(List<Long> reservationIds) {
		QStation departureStation = new QStation("departureStation");
		QStation arrivalStation = new QStation("arrivalStation");

		// 예약 조회 쿼리
		JPAQuery<ReservationProjection> query = queryFactory
			.select(new QReservationProjection(
				reservation.id,
				reservation.reservationCode,
				train.trainNumber,
				train.trainName,
				departureStation.stationName,
				arrivalStation.stationName,
				trainSchedule.departureTime,
				trainSchedule.arrivalTime,
				trainSchedule.operationDate,
				reservation.expiresAt,
				stationFare.standardFare,
				stationFare.firstClassFare
			))
			.from(reservation)
			.join(reservation.trainSchedule, trainSchedule)
			.join(trainSchedule.train, train)
			.join(reservation.departureStation, departureStation)
			.join(reservation.arrivalStation, arrivalStation)
			.join(stationFare).on(
				stationFare.departureStation.id.eq(reservation.departureStation.id)
					.and(stationFare.arrivalStation.id.eq(reservation.arrivalStation.id))
			)
			.orderBy(reservation.expiresAt.asc());

		if (reservationIds != null && !reservationIds.isEmpty()) {
			query.where(reservation.id.in(reservationIds));
		}

		// 예약 조회
		List<ReservationProjection> reservations = query.fetch();

		// 예약 좌석 조회
		Map<Long, List<SeatReservationProjection>> seats = queryFactory
			.select(new QSeatReservationProjection(
				seatReservation.id,
				seatReservation.reservation.id,
				seatReservation.passengerType,
				trainCar.carNumber,
				trainCar.carType,
				seat.seatRow.stringValue().concat(seat.seatColumn),
				Expressions.constant(0) // 임시 운임
			))
			.from(seatReservation)
			.leftJoin(seatReservation.seat, seat)
			.leftJoin(seat.trainCar, trainCar)
			.where(seatReservation.reservation.id.in(
				reservations.stream()
					.map(ReservationProjection::getReservationId)
					.toList()
			))
			.fetch()
			.stream()
			.collect(Collectors.groupingBy(SeatReservationProjection::getReservationId));

		return reservations.stream()
			.map(reservation -> ReservationInfo.of(
				reservation,
				seats.get(reservation.getReservationId()).stream()
					.map(seat -> seat.withFare(CarType.STANDARD.equals(seat.getCarType())
						? reservation.getStandardFare() : reservation.getFirstClassFare())
					).toList()
			)).toList();
	}
}
