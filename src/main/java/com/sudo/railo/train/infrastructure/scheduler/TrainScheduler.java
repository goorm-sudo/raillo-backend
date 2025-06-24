package com.sudo.railo.train.infrastructure.scheduler;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sudo.railo.train.application.TrainScheduleCreator;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrainScheduler {

	private final TrainScheduleCreator trainScheduleCreator;

	/**
	 * 매일 오전 2시에 운행 스케줄을 생성한다.
	 */
	@Scheduled(cron = "0 0 2 * * *")
	public void createTodayTrainSchedule() {
		trainScheduleCreator.createTrainSchedule(LocalDate.now());
	}
}
