package com.sudo.railo.booking.application;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.sudo.railo.booking.application.dto.response.TicketReadResponse;
import com.sudo.railo.booking.domain.Qr;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.Ticket;
import com.sudo.railo.booking.domain.status.TicketStatus;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infrastructure.ticket.TicketRepository;
import com.sudo.railo.booking.infrastructure.ticket.TicketRepositoryCustom;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.train.domain.Seat;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

	private final QrService qrService;
	private final MemberRepository memberRepository;
	private final TicketRepository ticketRepository;
	private final TicketRepositoryCustom ticketRepositoryCustom;

	/***
	 * 티켓을 생성하는 메서드
	 * @param reservation 예약 정보
	 * @param passengerType 승객 유형
	 */
	public void createTicket(Reservation reservation, Seat seat, PassengerType passengerType) {
		Qr qr = qrService.createQr();
		Ticket ticket = Ticket.builder()
			.reservation(reservation)
			.seat(seat)
			.qr(qr)
			.passengerType(passengerType)
			.ticketStatus(TicketStatus.ISSUED)
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
			// List<TicketReadResponse> tickets = ticketRepository.findByReservationMemberId(member.getId());
			return ticketRepositoryCustom.findPaidTicketResponsesByMemberId(member.getId());
		} catch (Exception e) {
			throw new BusinessException(BookingError.TICKET_LIST_GET_FAILED);
		}
	}

	public void deleteTicketById(Long ticketId) {
		Ticket ticket = ticketRepository.findById(ticketId)
			.orElseThrow(() -> new BusinessException(BookingError.TICKET_NOT_FOUND));
		ticketRepository.delete(ticket);
	}

	public void deleteTicketByReservationId(Long reservationId) {
		ticketRepository.deleteAllByReservationId(reservationId);
	}
}
