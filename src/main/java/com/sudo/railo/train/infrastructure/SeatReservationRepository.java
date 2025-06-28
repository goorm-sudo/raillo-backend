package com.sudo.railo.train.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sudo.railo.train.domain.SeatReservation;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {
	
}
