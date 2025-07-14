package com.sudo.railo.booking.application;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.sudo.railo.booking.application.dto.response.TicketReadResponse;
import com.sudo.railo.booking.domain.PassengerType;
import com.sudo.railo.booking.domain.PaymentStatus;
import com.sudo.railo.booking.domain.Qr;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.SeatReservation;
import com.sudo.railo.booking.domain.Ticket;
import com.sudo.railo.booking.domain.TicketStatus;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infra.TicketRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainCar;
import com.sudo.railo.train.domain.TrainSchedule;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

	private final QrService qrService;
	private final MemberRepository memberRepository;
	private final TicketRepository ticketRepository;

	/***
	 * 티켓을 생성하는 메서드
	 * @param reservation 예약 정보
	 * @param passengerType 승객 유형
	 */
	public void createTicket(Reservation reservation, SeatReservation seatReservation, PassengerType passengerType) {
		Qr qr = qrService.createQr();
		Ticket ticket = Ticket.builder()
			.reservation(reservation)
			.seatReservation(seatReservation)
			.qr(qr)
			.passengerType(passengerType)
			.paymentStatus(PaymentStatus.RESERVED)
			.status(TicketStatus.ISSUED)
			.build();
		try {
			ticketRepository.save(ticket);
		} catch (Exception e) {
			throw new BusinessException(BookingError.TICKET_CREATE_FAILED);
		}
	}

	public List<TicketReadResponse> getMyTickets(UserDetails userDetails) {
		Member member = memberRepository.findByMemberNo(userDetails.getUsername())
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
		try {
			List<Ticket> tickets = ticketRepository.findByReservationMemberId(member.getId());
			return tickets.stream()
				.map(ticket -> {
					Reservation reservation = ticket.getReservation();
					TrainSchedule trainSchedule = reservation.getTrainSchedule();
					Train train = trainSchedule.getTrain();
					SeatReservation seatReservation = ticket.getSeatReservation();
					Seat seat = seatReservation.getSeat();
					TrainCar trainCar = seat.getTrainCar();
					Station departureStation = seatReservation.getDepartureStation();
					Station arrivalStation = seatReservation.getArrivalStation();
					return new TicketReadResponse(
						ticket.getId(),
						reservation.getId(),
						seatReservation.getId(),
						trainSchedule.getOperationDate(),
						departureStation.getId(),
						departureStation.getStationName(),
						trainSchedule.getDepartureTime(),
						arrivalStation.getId(),
						arrivalStation.getStationName(),
						trainSchedule.getArrivalTime(),
						String.format("%03d", train.getTrainNumber()),
						train.getTrainName(),
						trainCar.getCarType(),
						trainCar.getCarNumber(),
						seat.getSeatRow(),
						seat.getSeatColumn(),
						seat.getSeatType()
					);
				})
				.toList();
		} catch (Exception e) {
			throw new BusinessException(BookingError.TICKET_LIST_GET_FAILED);
		}
	}
}
