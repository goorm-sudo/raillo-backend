package com.sudo.railo.booking.infra;

import static com.sudo.railo.booking.domain.QReservation.*;
import static com.sudo.railo.booking.domain.QSeatReservation.*;
import static com.sudo.railo.booking.domain.QTicket.*;
import static com.sudo.railo.train.domain.QSeat.*;
import static com.sudo.railo.train.domain.QTrain.*;
import static com.sudo.railo.train.domain.QTrainCar.*;
import static com.sudo.railo.train.domain.QTrainSchedule.*;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.booking.application.dto.response.TicketReadResponse;
import com.sudo.railo.booking.domain.PaymentStatus;
import com.sudo.railo.train.domain.QStation;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TicketRepositoryCustomImpl implements TicketRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<TicketReadResponse> findPaidTicketResponsesByMemberId(Long memberId) {
		QStation departureStation = new QStation("departureStation");
		QStation arrivalStation = new QStation("arrivalStation");

		return queryFactory
			.select(Projections.constructor(
				TicketReadResponse.class,
				ticket.id,
				reservation.id,
				seatReservation.id,
				trainSchedule.operationDate,
				departureStation.id,
				departureStation.stationName,
				trainSchedule.departureTime,
				arrivalStation.id,
				arrivalStation.stationName,
				trainSchedule.arrivalTime,
				Expressions.stringTemplate("LPAD(CAST({0} AS string), 3, '0')", train.trainNumber),
				train.trainName,
				trainCar.carType,
				trainCar.carNumber,
				seat.seatRow,
				seat.seatColumn,
				seat.seatType
			))
			.from(ticket)
			.join(ticket.reservation, reservation)
			.join(reservation.trainSchedule, trainSchedule)
			.join(trainSchedule.train, train)
			.join(ticket.seatReservation, seatReservation)
			.join(seatReservation.seat, seat)
			.join(seat.trainCar, trainCar)
			.join(seatReservation.departureStation, departureStation)
			.join(seatReservation.arrivalStation, arrivalStation)
			.where(reservation.member.id.eq(memberId).and(ticket.paymentStatus.eq(PaymentStatus.PAID)))
			.orderBy(trainSchedule.operationDate.desc(), trainSchedule.departureTime.desc())
			.fetch();
	}
}
