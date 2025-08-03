package com.sudo.railo.payment.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.booking.application.TicketService;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.infrastructure.reservation.ReservationRepository;
import com.sudo.railo.train.infrastructure.SeatReservationRepositoryCustom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentBookingService {

	private final ReservationRepository reservationRepository;
	private final SeatReservationRepositoryCustom seatReservationRepositoryCustom;
	private final TicketService ticketService;

	/**
	 * 예약 상태를 결제 완료로 변경
	 *
	 * @param reservation {@link Reservation} 객체
	 */
	@Transactional
	public void markReservationAsPaid(Reservation reservation) {
		reservation.approve();
		reservationRepository.save(reservation);

		log.info("예약 결제 완료 처리: reservationId={}", reservation.getId());
	}

	/**
	 * 티켓 발급
	 *
	 * @param reservation {@link Reservation} 객체
	 */
	@Transactional
	public void generateTicket(Reservation reservation) {
		seatReservationRepositoryCustom.findSeatInfoByReservationId(reservation.getId())
			.forEach(seatInfoProjection -> ticketService.createTicket(
				reservation, seatInfoProjection.getSeat(), seatInfoProjection.getPassengerType()));
	}
}
