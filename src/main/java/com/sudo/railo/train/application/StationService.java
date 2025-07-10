package com.sudo.railo.train.application;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.infrastructure.StationRepository;
import com.sudo.railo.train.infrastructure.jdbc.StationJdbcRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationService {

	private final StationRepository stationRepository;
	private final StationJdbcRepository stationJdbcRepository;

	@Transactional(readOnly = true)
	public Map<String, Station> getStationMap() {
		return stationRepository.findAll().stream()
			.collect(Collectors.toMap(Station::getStationName, Function.identity()));
	}

	/**
	 * 역 조회 및 저장
	 */
	@Transactional
	public Map<String, Station> findOrCreateStations(Set<String> stationNames) {
		Map<String, Station> stationMap = findExistingStations(stationNames);

		// 역 생성
		List<Station> stations = stationNames.stream()
			.filter(name -> !stationMap.containsKey(name))
			.map(Station::create)
			.toList();

		if (!stations.isEmpty()) {
			stationJdbcRepository.saveAll(stations);
			log.info("{}개의 역 저장 완료", stations.size());
		}

		// 역 ID가 없어서 다시 조회
		return getStationMap();
	}

	/**
	 * 이미 존재하는 역 조회
	 */
	private Map<String, Station> findExistingStations(Set<String> stationNames) {
		return stationRepository.findByStationNameIn(stationNames).stream()
			.collect(Collectors.toMap(Station::getStationName, Function.identity()));
	}
}
