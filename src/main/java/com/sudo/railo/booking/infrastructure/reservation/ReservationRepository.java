package com.sudo.railo.booking.infrastructure.reservation;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.status.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	void deleteAllByExpiresAtBeforeAndReservationStatusNot(LocalDateTime expiresAtBefore,
		ReservationStatus reservationStatus);
}
