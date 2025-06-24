package com.sudo.railo.train.presentation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.train.application.TrainScheduleService;
import com.sudo.railo.train.application.dto.request.OperationCalendarItem;
import com.sudo.railo.train.application.dto.response.TrainScheduleSuccess;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/train-schedule")
@RequiredArgsConstructor
@Tag(name = "열차 스케줄", description = "열차 스케줄 조회 API")
@Slf4j
public class TrainScheduleController {

	private final TrainScheduleService trainScheduleService;

	/**
	 * 운행 캘린더 조회
	 */
	@GetMapping("/calendar")
	@Operation(summary = "운행 캘린더 조회", description = "금일로부터 한 달간의 운행 캘린더를 조회합니다.")
	public SuccessResponse<List<OperationCalendarItem>> getOperationCalendar() {
		log.info("운행 캘린더 조회");
		List<OperationCalendarItem> calendar = trainScheduleService.getOperationCalendar();
		log.info("운행 캘린더 조회: {} 건", calendar.size());

		return SuccessResponse.of(TrainScheduleSuccess.OPERATION_CALENDAR_SUCCESS, calendar);
	}
}
