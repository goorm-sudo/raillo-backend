package com.sudo.railo.train.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sudo.railo.train.application.dto.excel.ScheduleStopData;
import com.sudo.railo.train.application.dto.excel.TrainScheduleData;
import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.infrastructure.ScheduleStopJdbcRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleStopService {

	private final ScheduleStopJdbcRepository scheduleStopJdbcRepository;

	/**
	 * 정차역 생성
	 */
	public void createScheduleStops(
		List<TrainSchedule> trainSchedules,
		List<TrainScheduleData> scheduleData,
		Map<String, Station> stationMap
	) {
		Map<String, TrainScheduleData> trainScheduleMap = new LinkedHashMap<>();
		for (TrainScheduleData data : scheduleData) {
			trainScheduleMap.putIfAbsent(data.getScheduleName(), data);
		}

		// 정차역 생성
		List<ScheduleStop> scheduleStops = trainSchedules.stream()
			.flatMap(schedule -> {
				TrainScheduleData data = trainScheduleMap.get(schedule.getScheduleName());
				return generateScheduleStops(data.getScheduleStopData(), stationMap, schedule).stream();
			})
			.toList();

		// 정차역 저장
		scheduleStopJdbcRepository.bulkInsert(scheduleStops);
		log.info("{}개의 정차역 저장 완료", scheduleStops.size());
	}

	public List<ScheduleStop> generateScheduleStops(
		List<ScheduleStopData> scheduleStopData,
		Map<String, Station> stationMap,
		TrainSchedule schedule
	) {
		List<ScheduleStop> scheduleStops = new ArrayList<>();
		for (ScheduleStopData stopData : scheduleStopData) {

			// 정차역 생성
			ScheduleStop scheduleStop = ScheduleStop.create(
				stopData.getStopOrder(),
				stopData.getArrivalTime(),
				stopData.getDepartureTime(),
				stationMap.get(stopData.getStationName()),
				schedule
			);
			scheduleStops.add(scheduleStop);

			// 연관 관계 설정
			scheduleStop.setTrainSchedule(schedule);
		}
		return scheduleStops;
	}
}
