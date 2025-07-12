package com.sudo.railo.train.presentation;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.train.application.StationFareCreator;
import com.sudo.railo.train.application.TrainScheduleCreator;
import com.sudo.railo.train.application.dto.response.TrainAdminSuccess;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/trains")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class TrainAdminController {

	private final TrainScheduleCreator trainScheduleCreator;
	private final StationFareCreator stationFareCreator;

	@PostMapping("/parse")
	public SuccessResponse<?> parseExcel() {
		// 스케줄 파싱
		trainScheduleCreator.parseTrainSchedule();

		// 운임표 파싱
		stationFareCreator.parseStationFare();

		return SuccessResponse.of(TrainAdminSuccess.TRAIN_PARSE_SUCCESS);
	}

	@PostMapping("/generate/day")
	public SuccessResponse<?> generateDaySchedule() {
		trainScheduleCreator.createTrainSchedule();
		return SuccessResponse.of(TrainAdminSuccess.TRAIN_SCHEDULE_CREATED);
	}

	@PostMapping("/generate/month")
	public SuccessResponse<?> generateMonthSchedule() {
		LocalDate startDate = LocalDate.now();
		LocalDate endDate = startDate.plusMonths(1).plusDays(1);

		List<LocalDate> dates = startDate.datesUntil(endDate).toList();

		trainScheduleCreator.createTrainSchedule(dates);
		return SuccessResponse.of(TrainAdminSuccess.TRAIN_SCHEDULE_CREATED);
	}
}
