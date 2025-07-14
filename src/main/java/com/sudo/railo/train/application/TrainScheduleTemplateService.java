package com.sudo.railo.train.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.excel.ScheduleStopData;
import com.sudo.railo.train.application.dto.excel.TrainScheduleData;
import com.sudo.railo.train.domain.ScheduleStopTemplate;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainScheduleTemplate;
import com.sudo.railo.train.infrastructure.ScheduleStopTemplateRepository;
import com.sudo.railo.train.infrastructure.TrainScheduleTemplateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainScheduleTemplateService {

	private final TrainService trainService;
	private final StationService stationService;
	private final TrainScheduleTemplateRepository trainScheduleTemplateRepository;
	private final ScheduleStopTemplateRepository scheduleStopTemplateRepository;

	/**
	 * 스케줄 템플릿 조회
	 */
	public List<TrainScheduleTemplate> findTrainScheduleTemplate() {
		return trainScheduleTemplateRepository.findAllWithScheduleStops();
	}

	/**
	 * 스케줄 템플릿 저장
	 */
	@Transactional
	public void createTrainScheduleTemplate(List<TrainScheduleData> trainScheduleData,
		Map<String, Station> stationMap, Map<Integer, Train> trainMap) {

		// 스케줄 템플릿 삭제
		deleteAllTrainSchedule();

		// 스케줄 템플릿 생성
		List<TrainScheduleTemplate> trainScheduleTemplates = new ArrayList<>();
		trainScheduleData.forEach(data -> {
			try {
				trainScheduleTemplates.add(createTrainScheduleTemplate(data, stationMap, trainMap));
			} catch (IllegalArgumentException ex) {
				log.warn("스케줄 생성에 실패했습니다. scheduleName={}, reason={}", data.getScheduleName(), ex.getMessage());
			}
		});

		// 스케줄 템플릿 저장
		trainScheduleTemplateRepository.saveAll(trainScheduleTemplates);
		log.info("{}개의 스케줄 템플릿 저장 완료", trainScheduleTemplates.size());
	}

	/**
	 * 스케줄 템플릿 삭제 메서드
	 */
	private void deleteAllTrainSchedule() {
		scheduleStopTemplateRepository.deleteAllInBatch();
		trainScheduleTemplateRepository.deleteAllInBatch();
	}

	/**
	 * 스케줄 템플릿 생성 메서드
	 */
	private TrainScheduleTemplate createTrainScheduleTemplate(TrainScheduleData data,
		Map<String, Station> stationMap, Map<Integer, Train> trainMap) {

		ScheduleStopData firstStop = data.getFirstStop();
		ScheduleStopData lastStop = data.getLastStop();

		Station departureStation = stationService.getStationByName(firstStop.getStationName(), stationMap);
		Station arrivalStation = stationService.getStationByName(lastStop.getStationName(), stationMap);

		return TrainScheduleTemplate.create(
			data.getScheduleName(),
			data.getOperatingDay(),
			firstStop.getDepartureTime(),
			lastStop.getArrivalTime(),
			trainService.getTrainByNumber(data.getTrainData().getTrainNumber(), trainMap),
			departureStation,
			arrivalStation,
			createScheduleStopTemplates(data.getScheduleStopData(), stationMap)
		);
	}

	/**
	 * 정차역 템플릿 생성
	 */
	private List<ScheduleStopTemplate> createScheduleStopTemplates(
		List<ScheduleStopData> stopDataList, Map<String, Station> stationMap) {

		return stopDataList.stream()
			.map(data -> ScheduleStopTemplate.create(
				data.getStopOrder(),
				data.getArrivalTime(),
				data.getDepartureTime(),
				stationService.getStationByName(data.getStationName(), stationMap)
			))
			.toList();
	}
}
