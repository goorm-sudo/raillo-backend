package com.sudo.railo.train.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
	public void createStationFare() {
		try {
			log.info("운임표 생성 시작");

			List<Sheet> sheets = parser.getSheets();
			Set<StationFareData> stationFareData = new HashSet<>();
			sheets.forEach(sheet -> {
				log.info("{} 시트 파싱 시작", sheet.getSheetName());

				StationFareHeader header = parser.getHeader(sheet);
				List<StationFareData> data = parser.getStationFareData(sheet, header);
				stationFareData.addAll(data);

				log.info("{} 시트 파싱 종료", sheet.getSheetName());
			});

			persistStationFare(stationFareData);

			log.info("운임표 생성 완료");
		} catch (Exception ex) {
			log.error("운임표 생성 중 예외가 발생했습니다", ex);
		}
	}

	private void persistStationFare(Set<StationFareData> stationFareData) {
		List<String> stationNames = stationFareData.stream()
			.flatMap(data -> Stream.of(data.departureStation(), data.arrivalStation()))
			.distinct()
			.toList();
		Map<String, Station> stations = stationService.findOrCreateStation(stationNames);

		List<StationFare> stationFares = new ArrayList<>();
		stationFares.addAll(stationFareData.stream()
			.map(data -> StationFare.create(
				stations.get(data.departureStation()),
				stations.get(data.arrivalStation()),
				data.standardFare(),
				data.firstClassFare()
			)).toList());

		// TODO: 역방향 운임
		stationFares.addAll(stationFareData.stream()
			.map(data -> StationFare.create(
				stations.get(data.arrivalStation()),
				stations.get(data.departureStation()),
				data.standardFare(),
				data.firstClassFare()
			)).toList());

		stationFareRepository.saveAll(stationFares);
		log.info("{}개 운임 데이터 저장 완료", stationFares.size());
	}
}
