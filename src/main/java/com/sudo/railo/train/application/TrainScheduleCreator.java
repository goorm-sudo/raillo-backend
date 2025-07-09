package com.sudo.railo.train.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.excel.ScheduleStopData;
import com.sudo.railo.train.application.dto.excel.TrainData;
import com.sudo.railo.train.application.dto.excel.TrainScheduleData;
import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.infrastructure.TrainScheduleRepository;
import com.sudo.railo.train.infrastructure.excel.TrainScheduleParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainScheduleCreator {

	private final TrainScheduleParser parser;
	private final StationService stationService;
	private final TrainService trainService;
	private final TrainScheduleRepository trainScheduleRepository;

	@Transactional
	public void createStations() {
		List<Sheet> sheets = parser.getSheets();
		Set<String> stationNames = new LinkedHashSet<>();

		for (Sheet sheet : sheets) {
			List<CellAddress> addresses = getFirstCellAddresses(sheet);
			for (CellAddress address : addresses) {
				stationNames.addAll(parser.parseStationNames(sheet, address));
			}
		}

		stationService.createStations(stationNames);
	}

	@Transactional
	public void createTrains() {
		List<Sheet> sheets = parser.getSheets();
		List<TrainData> trainData = new ArrayList<>();

		for (Sheet sheet : sheets) {
			List<CellAddress> addresses = getFirstCellAddresses(sheet);
			for (CellAddress address : addresses) {
				trainData.addAll(parser.parseTrain(sheet, address));
			}
		}

		trainService.createTrains(trainData);
	}

	@Transactional
	public void createTrainSchedule() {
		LocalDate localDate = trainScheduleRepository.findLastOperationDate()
			.map(date -> date.plusDays(1))
			.orElse(LocalDate.now());

		createTrainSchedule(localDate);
	}

	@Transactional
	public void createTrainSchedule(LocalDate localDate) {
		if (trainScheduleRepository.existsByOperationDate(localDate)) {
			log.info("[{}] 이미 운행 스케줄이 존재합니다.", localDate);
			return;
		}

		List<Sheet> sheets = parser.getSheets();
		List<TrainScheduleData> trainScheduleData = new ArrayList<>();

		for (Sheet sheet : sheets) {
			List<CellAddress> addresses = getFirstCellAddresses(sheet);
			for (CellAddress address : addresses) {
				trainScheduleData.addAll(parser.parseTrainSchedule(sheet, address, localDate));
			}
		}

		// 역, 열차 조회
		Map<String, Station> stationMap = stationService.getStationMap();
		Map<Integer, Train> trainMap = trainService.getTrainMap();

		// 스케줄 생성
		List<TrainSchedule> trainSchedules = trainScheduleData.stream()
			.map(data -> createTrainSchedule(data, trainMap, stationMap))
			.toList();

		trainScheduleRepository.saveAll(trainSchedules);
		log.info("[{}] {}개의 운행 스케줄 저장 완료", localDate, trainSchedules.size());
	}

	/**
	 * 하행과 상행 파싱 시작 지점 반환
	 */
	private List<CellAddress> getFirstCellAddresses(Sheet sheet) {
		CellAddress downTrainAddress = parser.getFirstCellAddress(sheet, 0); // 하행
		CellAddress upTrainAddress = parser.getFirstCellAddress(sheet, downTrainAddress.getColumn() + 1); // 상행
		return List.of(downTrainAddress, upTrainAddress);
	}

	private TrainSchedule createTrainSchedule(TrainScheduleData data, Map<Integer, Train> trainMap,
		Map<String, Station> stationMap) {
		Train train = trainMap.get(data.getTrainData().getTrainNumber());
		ScheduleStopData firstStop = data.getFirstStop();
		ScheduleStopData lastStop = data.getLastStop();

		List<ScheduleStop> scheduleStops = data.getScheduleStopData().stream()
			.map(scheduleStopData -> ScheduleStop.create(
				scheduleStopData.getStopOrder(),
				scheduleStopData.getArrivalTime(),
				scheduleStopData.getDepartureTime(),
				stationMap.get(scheduleStopData.getStationName())
			)).toList();

		return TrainSchedule.create(
			data.getScheduleName(),
			data.getOperationDate(),
			firstStop.getDepartureTime(),
			lastStop.getArrivalTime(),
			train,
			stationMap.get(firstStop.getStationName()),
			stationMap.get(lastStop.getStationName()),
			scheduleStops
		);
	}
}
