package com.sudo.railo.train.presentation;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sudo.railo.train.application.StationFareCreator;
import com.sudo.railo.train.application.TrainScheduleCreator;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TrainTestController {

	private final TrainScheduleCreator trainScheduleCreator;
	private final StationFareCreator stationFareCreator;

	@PostMapping("/train-schedule")
	public ResponseEntity<?> createTrainSchedule() {
		LocalDate startDate = LocalDate.now();
		LocalDate endDate = startDate.plusMonths(1);

		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			trainScheduleCreator.createTrainSchedule(date);
		}

		return ResponseEntity.ok("열차 스케줄 생성");
	}

	@PostMapping("/train-schedule/today")
	public ResponseEntity<?> createTodayTrainSchedule() {
		trainScheduleCreator.createTrainSchedule(LocalDate.now());

		return ResponseEntity.ok("열차 스케줄 생성");
	}

	@PostMapping("/station-fare")
	public ResponseEntity<?> createStationFare() {
		stationFareCreator.createStationFare();

		return ResponseEntity.ok("운임표 생성");
	}
}
