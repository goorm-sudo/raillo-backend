package com.sudo.railo.booking.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
	List<Ticket> findByReservation(Reservation reservation);
}
