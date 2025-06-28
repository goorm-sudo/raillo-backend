package com.sudo.railo.booking.infra;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.sudo.railo.booking.domain.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	@Modifying(clearAutomatically = true)
	void deleteAllByReservedAtBefore(LocalDateTime reservedAtBefore);
}
