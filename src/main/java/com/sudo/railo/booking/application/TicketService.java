package com.sudo.railo.booking.application;

import org.springframework.stereotype.Service;

import com.sudo.railo.booking.domain.PassengerType;
import com.sudo.railo.booking.domain.PaymentStatus;
import com.sudo.railo.booking.domain.Qr;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.Ticket;
import com.sudo.railo.booking.domain.TicketStatus;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infra.TicketRepository;
import com.sudo.railo.global.exception.error.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

	private final QrService qrService;
	private final TicketRepository ticketRepository;

	/***
	 * 티켓을 생성하는 메서드
	 * @param reservation 예약 정보
	 * @param passengerType 승객 유형
	 */
	public void createTicket(Reservation reservation, PassengerType passengerType) {
		Qr qr = qrService.createQr();
		Ticket ticket = Ticket.builder()
			.reservation(reservation)
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
}
