package com.sudo.railo.booking.infra;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.booking.domain.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	void deleteAllByExpiresAtBefore(LocalDateTime expiresAtBefore);
}
