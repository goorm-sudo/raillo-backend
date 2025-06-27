package com.sudo.railo.train.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.infrastructure.StationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StationService {

	private final StationRepository stationRepository;

	@Transactional
	public Map<String, Station> findOrCreateStation(List<String> stationNames) {
		Map<String, Station> stationMap = stationRepository.findByStationNameIn(stationNames).stream()
			.collect(Collectors.toMap(Station::getStationName, station -> station));

		List<Station> newStations = stationNames.stream()
			.filter(name -> !stationMap.containsKey(name))
			.map(Station::create)
			.toList();

		if (!newStations.isEmpty()) {
			stationRepository.saveAll(newStations);
			newStations.forEach(station -> stationMap.put(station.getStationName(), station));
		}
		return stationMap;
	}
}
