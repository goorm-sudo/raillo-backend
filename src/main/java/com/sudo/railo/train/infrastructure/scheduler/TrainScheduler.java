package com.sudo.railo.train.infrastructure.scheduler;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sudo.railo.train.application.TrainScheduleCreator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrainScheduler {

	private final TrainScheduleCreator trainScheduleCreator;

	/**
	 * 매일 오전 2시에 운행 스케줄을 생성한다.
	 */
	@Scheduled(cron = "0 0 2 * * *")
	public void createTodayTrainSchedule() {
		try {
			trainScheduleCreator.createTrainSchedule(LocalDate.now());
		} catch (Exception ex) {
			log.error("운행 스케줄 생성 중 오류가 발생했습니다.", ex);
		}
	}
}
