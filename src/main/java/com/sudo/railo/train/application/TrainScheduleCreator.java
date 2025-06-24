package com.sudo.railo.train.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.ScheduleStopData;
import com.sudo.railo.train.application.dto.TrainScheduleData;
import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.infrastructure.excel.TrainScheduleParser;
import com.sudo.railo.train.infrastructure.persistence.TrainScheduleRepository;

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

	@Value("${train.schedule.excel.path}")
	private String trainSchedulePath;

	@Transactional
	public void createTrainSchedule(LocalDate localDate) {
		List<Sheet> sheets = parser.getSheets(trainSchedulePath);
		sheets.forEach(sheet -> {
			// 하행
			CellAddress downTrainAddress = parser.getFirstCellAddress(sheet, 0);
			parseAndPersistTrainSchedule(sheet, downTrainAddress, localDate);

			// 상행
			CellAddress upTrainAddress = parser.getFirstCellAddress(sheet, downTrainAddress.getColumn() + 1);
			parseAndPersistTrainSchedule(sheet, upTrainAddress, localDate);
		});
	}

	private void parseAndPersistTrainSchedule(Sheet sheet, CellAddress address, LocalDate localDate) {
		List<String> stationNames = parser.getStationNames(sheet, address);
		Map<String, Station> stationMap = stationService.findOrCreateStation(stationNames);

		List<TrainScheduleData> trainScheduleData = parser.getTrainScheduleData(sheet, address, localDate);
		Map<Integer, Train> trainMap = trainService.findOrCreateTrains(trainScheduleData.stream()
			.map(TrainScheduleData::getTrainData)
			.toList());

		List<TrainSchedule> trainSchedules = trainScheduleData.stream()
			.map(data -> createTrainSchedule(data, trainMap, stationMap))
			.toList();
		trainScheduleRepository.saveAll(trainSchedules);
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
