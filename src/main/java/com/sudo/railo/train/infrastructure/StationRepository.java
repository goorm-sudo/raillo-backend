package com.sudo.railo.train.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sudo.railo.train.domain.Station;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

	Optional<Station> findByStationName(String stationName);

	List<Station> findByStationNameContaining(String keyword);
}
