package com.sudo.railo.train.presentation;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.train.application.TrainScheduleService;
import com.sudo.railo.train.application.TrainSearchApplicationService;
import com.sudo.railo.train.application.dto.request.TrainCarListRequest;
import com.sudo.railo.train.application.dto.request.TrainSearchRequest;
import com.sudo.railo.train.application.dto.response.OperationCalendarItem;
import com.sudo.railo.train.application.dto.response.TrainCarListResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchSlicePageResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchSuccess;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/train-search")
@RequiredArgsConstructor
@Tag(name = "열차 스케줄", description = "열차 스케줄 조회 API")
@Slf4j
public class TrainSearchController {

	private final TrainScheduleService trainScheduleService;
	private final TrainSearchApplicationService trainSearchApplicationService;

	/**
	 * 운행 캘린더 조회
	 */
	@GetMapping("/calendar")
	@Operation(summary = "운행 캘린더 조회", description = "금일로부터 한 달간의 운행 캘린더를 조회합니다.")
	public SuccessResponse<List<OperationCalendarItem>> getOperationCalendar() {
		log.info("운행 캘린더 조회");
		List<OperationCalendarItem> calendar = trainScheduleService.getOperationCalendar();
		log.info("운행 캘린더 조회: {} 건", calendar.size());

		return SuccessResponse.of(TrainSearchSuccess.OPERATION_CALENDAR_SUCCESS, calendar);
	}

	/**
	 * 열차 스케줄 검색
	 */
	@PostMapping("/search")
	@Operation(
		summary = "열차 스케줄 검색",
		description = "출발역, 도착역, 운행 날짜로 열차 스케줄 검색"
	)
	public SuccessResponse<TrainSearchSlicePageResponse> searchTrainSchedules(
		@Valid @RequestBody TrainSearchRequest request,
		@PageableDefault(size = 20, sort = "departureTime")
		@Parameter(description = "페이징 정보 (page, size)", hidden = true) Pageable pageable) {

		log.info("열차 검색 요청: {} -> {}, {}, 승객: {}명, 출발 시간: {}시 이후, page: {}, size: {}",
			request.departureStationId(), request.arrivalStationId(),
			request.operationDate(), request.passengerCount(), request.departureHour(),
			pageable.getPageNumber(), pageable.getPageSize());

		TrainSearchSlicePageResponse response = trainScheduleService.searchTrains(request, pageable);

		return SuccessResponse.of(TrainSearchSuccess.TRAIN_SEARCH_SUCCESS, response);
	}

	/**
	 * 열차 객차 목록 조회 (잔여 좌석이 있는 객차만)
	 */
	@PostMapping("/cars")
	@Operation(
		summary = "열차 객차 목록 조회",
		description = "선택한 열차의 잔여 좌석이 있는 객차 목록을 조회합니다. 프론트엔드에서 객차 선택용 드롭다운에 사용됩니다."
	)
	public SuccessResponse<TrainCarListResponse> getAvailableTrainCars(
		@Valid @RequestBody TrainCarListRequest request) {

		log.info("열차 객차 목록 조회 요청: trainScheduleId={}, {}역 -> {}역, 승객={}명",
			request.trainScheduleId(), request.departureStationId(),
			request.arrivalStationId(), request.passengerCount());

		TrainCarListResponse response = trainSearchApplicationService.getAvailableTrainCars(request);

		log.info("열차 객차 목록 조회 완료: {}개 객차 조회됨, 추천 객차={}",
			response.carInfos().size(), response.recommendedCarNumber());

		return SuccessResponse.of(TrainSearchSuccess.TRAIN_CAR_LIST_SUCCESS, response);
	}
}
