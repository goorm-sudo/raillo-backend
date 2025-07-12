package com.sudo.railo.train.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.domain.TrainScheduleTemplate;
import com.sudo.railo.train.infrastructure.jdbc.ScheduleStopJdbcRepository;

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
		List<TrainScheduleTemplate> templates
	) {
		Map<String, TrainScheduleTemplate> templateMap = new LinkedHashMap<>();
		for (TrainScheduleTemplate template : templates) {
			templateMap.put(template.getScheduleName(), template);
		}

		// 정차역 생성
		List<ScheduleStop> scheduleStops = trainSchedules.stream()
			.flatMap(schedule -> {
				TrainScheduleTemplate template = templateMap.get(schedule.getScheduleName());
				return template.getScheduleStops().stream()
					.map(t -> ScheduleStop.create(t, schedule));
			})
			.toList();

		// 정차역 저장
		scheduleStopJdbcRepository.saveAll(scheduleStops);
		log.info("{}개의 정차역 저장 완료", scheduleStops.size());
	}
}
