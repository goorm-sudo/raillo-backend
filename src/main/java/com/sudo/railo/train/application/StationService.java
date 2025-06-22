package com.sudo.railo.train.application;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.infrastructure.persistence.StationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StationService {

	private final StationRepository stationRepository;

	public List<Station> getStations(List<String> stationNames) {
		return stationRepository.findByStationNameIn(stationNames);
	}

	public List<Station> saveStationsIfNotExists(List<String> stationNames) {
		List<Station> stations = getStations(stationNames);
		Set<String> savedStationNames = stations.stream()
			.map(Station::getStationName)
			.collect(Collectors.toSet());

		return stationRepository.saveAll(stationNames.stream()
			.filter(stationName -> !savedStationNames.contains(stationName))
			.map(Station::create)
			.toList());
	}
}
