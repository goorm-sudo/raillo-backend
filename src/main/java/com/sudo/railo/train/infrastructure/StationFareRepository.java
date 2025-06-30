package com.sudo.railo.train.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.train.domain.StationFare;

public interface StationFareRepository extends JpaRepository<StationFare, Long> {

}
