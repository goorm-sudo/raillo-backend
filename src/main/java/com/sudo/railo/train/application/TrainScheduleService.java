package com.sudo.railo.train.application;

import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.ScheduleStopDto;
import com.sudo.railo.train.application.dto.TrainScheduleDto;
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
public class TrainScheduleService {

	private final TrainScheduleParser parser;
	private final StationService stationService;
	private final TrainService trainService;
	private final TrainScheduleRepository trainScheduleRepository;

	@Transactional
	public void createTrainSchedule(String path) {
		List<Sheet> sheets = parser.getSheets(path);
		sheets.forEach(sheet -> {
			// 하행
			CellAddress downTrainAddress = parser.getFirstCellAddress(sheet, 0);
			parseAndPersistTrainSchedule(sheet, downTrainAddress);

			// 상행
			CellAddress upTrainAddress = parser.getFirstCellAddress(sheet, downTrainAddress.getColumn() + 1);
			parseAndPersistTrainSchedule(sheet, upTrainAddress);
		});
	}

	private void parseAndPersistTrainSchedule(Sheet sheet, CellAddress address) {
		List<String> stationNames = parser.getStationNames(sheet, address);
		Map<String, Station> stationMap = stationService.findOrCreateStation(stationNames);

		List<TrainScheduleDto> trainScheduleDtos = parser.getTrainScheduleDtos(sheet, address);
		Map<Integer, Train> trainMap = trainService.findOrCreateTrains(trainScheduleDtos.stream()
			.map(TrainScheduleDto::getTrainDto)
			.toList());

		List<TrainSchedule> trainSchedules = trainScheduleDtos.stream()
			.map(dto -> createTrainSchedule(dto, trainMap, stationMap))
			.toList();
		trainScheduleRepository.saveAll(trainSchedules);
	}

	private TrainSchedule createTrainSchedule(TrainScheduleDto dto, Map<Integer, Train> trainMap,
		Map<String, Station> stationMap) {
		Train train = trainMap.get(dto.getTrainDto().getTrainNumber());
		ScheduleStopDto firstStop = dto.getFirstStop();
		ScheduleStopDto lastStop = dto.getLastStop();

		List<ScheduleStop> scheduleStops = dto.getScheduleStopDtos().stream()
			.map(scheduleStopDto -> ScheduleStop.create(
				scheduleStopDto.getStopOrder(),
				scheduleStopDto.getArrivalTime(),
				scheduleStopDto.getDepartureTime(),
				stationMap.get(scheduleStopDto.getStationName())
			)).toList();

		return TrainSchedule.create(
			dto.getScheduleName(),
			dto.getOperationDate(),
			firstStop.getDepartureTime(),
			lastStop.getArrivalTime(),
			train,
			stationMap.get(firstStop.getStationName()),
			stationMap.get(lastStop.getStationName()),
			scheduleStops
		);
	}
}
