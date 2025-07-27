package com.sudo.railo.booking.infrastructure.reservation;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.status.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	List<Reservation> findAllByExpiresAtBeforeAndReservationStatus(
		LocalDateTime expiresAtBefore,
		ReservationStatus reservationStatus,
		Pageable pageable
	);
}
