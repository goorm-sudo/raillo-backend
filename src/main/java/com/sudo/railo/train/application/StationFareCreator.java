package com.sudo.railo.train.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.excel.StationFareData;
import com.sudo.railo.train.application.dto.excel.StationFareHeader;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.StationFare;
import com.sudo.railo.train.infrastructure.StationFareRepository;
import com.sudo.railo.train.infrastructure.excel.StationFareParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationFareCreator {

	private final StationFareParser parser;
	private final StationService stationService;
	private final StationFareRepository stationFareRepository;

	@Transactional
	public void parseStationFare() {
		log.info("운임표 파싱 시작");

		List<Sheet> sheets = parser.getSheets();
		Set<StationFareData> stationFareData = new HashSet<>();

		for (Sheet sheet : sheets) {
			// 운임표 헤더 위치 파싱
			StationFareHeader header = parser.getHeader(sheet);

			// 운임표 파싱
			List<StationFareData> data = parser.getStationFareData(sheet, header);
			stationFareData.addAll(data);
		}

		// 운임표 저장
		persistStationFare(stationFareData);

		log.info("운임표 파싱 종료");
	}

	private void persistStationFare(Set<StationFareData> stationFareData) {
		// 역 조회 및 저장
		Map<String, Station> stationMap = stationService.getStationMap();

		// 운임표 삭제
		deleteAllStationFare();

		List<StationFare> stationFares = new ArrayList<>();
		stationFareData.forEach(data -> {

			Station departureStation = stationService.getStationByName(data.departureStation(), stationMap);
			Station arrivalStation = stationService.getStationByName(data.arrivalStation(), stationMap);

			// 순방향 운임
			stationFares.add(StationFare.create(
				departureStation,
				arrivalStation,
				data.standardFare(),
				data.firstClassFare()
			));

			// 역방향 운임
			stationFares.add(StationFare.create(
				arrivalStation,
				departureStation,
				data.standardFare(),
				data.firstClassFare()
			));
		});

		stationFareRepository.saveAll(stationFares);
		log.info("{}개 운임 데이터 저장 완료", stationFares.size());
	}

	/**
	 * 운임표 삭제 메서드
	 */
	private void deleteAllStationFare() {
		stationFareRepository.deleteAllInBatch();
	}
}
