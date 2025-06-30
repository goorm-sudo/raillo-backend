package com.sudo.railo.train.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.train.domain.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {
}
