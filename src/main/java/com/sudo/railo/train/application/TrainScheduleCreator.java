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
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.infrastructure.TrainScheduleRepository;
import com.sudo.railo.train.infrastructure.excel.TrainScheduleParser;
import com.sudo.railo.train.infrastructure.jdbc.TrainScheduleJdbcRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainScheduleCreator {

	private final TrainScheduleParser parser;
	private final StationService stationService;
	private final TrainService trainService;
	private final ScheduleStopService scheduleStopService;
	private final TrainScheduleRepository trainScheduleRepository;
	private final TrainScheduleJdbcRepository trainScheduleJdbcRepository;

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

		createTrainSchedule(List.of(localDate));
	}

	@Transactional
	public void createTrainSchedule(List<LocalDate> dates) {
		List<TrainScheduleData> scheduleData = parseSchedules(dates);
		Map<String, Station> stationMap = stationService.getStationMap();
		Map<Integer, Train> trainMap = trainService.getTrainMap();

		// 스케줄 생성
		List<TrainSchedule> trainSchedules = scheduleData.stream()
			.map(data -> createTrainSchedule(data, trainMap, stationMap))
			.toList();

		if (!trainSchedules.isEmpty()) {
			// 스케줄 저장
			trainScheduleJdbcRepository.bulkInsert(trainSchedules);
			log.info("{}개의 운행 스케줄 저장 완료", trainSchedules.size());

			// 정차역 생성
			scheduleStopService.createScheduleStops(
				fetchTrainSchedules(trainSchedules, dates),
				scheduleData,
				stationMap
			);
		}
	}

	/**
	 * 스케줄 파싱
	 */
	private List<TrainScheduleData> parseSchedules(List<LocalDate> dates) {
		List<TrainScheduleData> scheduleData = new ArrayList<>();
		List<Sheet> sheets = parser.getSheets();

		// 운행 스케줄이 존재하는 날짜 조회
		Set<LocalDate> existingDates = trainScheduleRepository.findExistingOperationDatesIn(dates);

		dates.forEach(date -> {
			if (existingDates.contains(date)) {
				log.info("[{}] 이미 운행 스케줄이 존재합니다.", date);
				return;
			}

			for (Sheet sheet : sheets) {
				List<CellAddress> addresses = getFirstCellAddresses(sheet);
				for (CellAddress address : addresses) {
					scheduleData.addAll(parser.parseTrainSchedule(sheet, address, date));
				}
			}
		});
		return scheduleData;
	}

	/**
	 * 스케줄 ID를 가져오기 위한 메서드
	 */
	private List<TrainSchedule> fetchTrainSchedules(List<TrainSchedule> schedules, List<LocalDate> dates) {
		List<String> scheduleNames = schedules.stream()
			.map(TrainSchedule::getScheduleName)
			.toList();

		return trainScheduleRepository.findByScheduleNameInAndOperationDateIn(scheduleNames, dates);
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

		return TrainSchedule.create(
			data.getScheduleName(),
			data.getOperationDate(),
			firstStop.getDepartureTime(),
			lastStop.getArrivalTime(),
			train,
			stationMap.get(firstStop.getStationName()),
			stationMap.get(lastStop.getStationName())
		);
	}
}
