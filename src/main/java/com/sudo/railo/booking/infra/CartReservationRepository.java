package com.sudo.railo.booking.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sudo.railo.booking.domain.CartReservation;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.member.domain.Member;

public interface CartReservationRepository extends JpaRepository<CartReservation, Long> {

	boolean existsByReservation(Reservation reservation);

	@Query("SELECT cr.reservation.id FROM CartReservation cr WHERE cr.member = :member")
	List<Long> findReservationIdsByMember(Member member);
}
