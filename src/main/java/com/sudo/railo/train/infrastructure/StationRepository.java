package com.sudo.railo.train.infrastructure;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.train.domain.Station;

public interface StationRepository extends JpaRepository<Station, Long> {

	List<Station> findByStationNameIn(Collection<String> stationNames);
}
