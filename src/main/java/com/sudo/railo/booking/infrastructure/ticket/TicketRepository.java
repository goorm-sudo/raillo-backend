package com.sudo.railo.booking.infrastructure.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.booking.domain.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
	List<Ticket> findByReservationMemberId(Long reservationMemberId);

	void deleteAllByReservationId(Long reservationId);
}
