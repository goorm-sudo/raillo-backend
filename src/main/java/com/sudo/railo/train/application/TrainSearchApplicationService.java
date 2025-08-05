package com.sudo.railo.train.application;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.TrainScheduleBasicInfo;
import com.sudo.railo.train.application.dto.request.TrainCarListRequest;
import com.sudo.railo.train.application.dto.request.TrainCarSeatDetailRequest;
import com.sudo.railo.train.application.dto.request.TrainSearchRequest;
import com.sudo.railo.train.application.dto.response.OperationCalendarItem;
import com.sudo.railo.train.application.dto.response.TrainCarInfo;
import com.sudo.railo.train.application.dto.response.TrainCarListResponse;
import com.sudo.railo.train.application.dto.response.TrainCarSeatDetailResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchSlicePageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TrainSearchApplicationService {

	private final TrainSearchService trainSearchService;
	private final TrainSeatQueryService trainCarService;

	/**
	 * 운행 캘린더 조회
	 */
	public List<OperationCalendarItem> getOperationCalendar() {
		return trainSearchService.getOperationCalendar();
	}

	/**
	 * 통합 열차 조회 (열차 스케줄 검색)
	 */
	public TrainSearchSlicePageResponse searchTrains(TrainSearchRequest request, Pageable pageable) {
		TrainSearchSlicePageResponse response = trainSearchService.searchTrains(request, pageable);

		log.info("열차 검색 완료: {} 건 조회, hasNext: {}", response.numberOfElements(), response.hasNext());

		return response;
	}

	/**
	 * 열차 객차 목록 조회 (잔여 좌석이 있는 객차만)
	 */
	public TrainCarListResponse getAvailableTrainCars(TrainCarListRequest request) {
		log.info("열차 객차 목록 조회: trainScheduleId={}, {}역 -> {}역, 승객={}명",
			request.trainScheduleId(), request.departureStationId(),
			request.arrivalStationId(), request.passengerCount());

		// 1. 열차 스케줄 기본 정보 조회
		TrainScheduleBasicInfo scheduleInfo = trainSearchService.getTrainScheduleBasicInfo(request.trainScheduleId());

		// 2. 잔여 좌석이 있는 객차 목록 조회
		List<TrainCarInfo> availableCars = trainCarService.getAvailableTrainCars(
			request.trainScheduleId(), request.departureStationId(), request.arrivalStationId());

		// 3. 승객 수에 맞는 추천 객차 선택 (Application Service 책임)
		String recommendedCarNumber = selectRecommendedCar(availableCars, request.passengerCount());

		log.info("열차 객차 목록 조회 완료: {}개 객차, 추천 객차={}, 열차={}-{}",
			availableCars.size(), recommendedCarNumber,
			scheduleInfo.trainClassificationCode(), scheduleInfo.trainNumber());

		return TrainCarListResponse.of(
			request.trainScheduleId(),
			recommendedCarNumber,
			availableCars.size(),
			scheduleInfo.trainClassificationCode(),
			scheduleInfo.trainNumber(),
			availableCars
		);
	}

	/**
	 * 열차 객차 좌석 상세 조회
	 */
	public TrainCarSeatDetailResponse getTrainCarSeatDetail(TrainCarSeatDetailRequest request) {
		log.info("열차 객차 좌석 상세 조회: trainCarId={}, trainScheduleId={}, {}역 -> {}역",
			request.trainCarId(), request.trainScheduleId(),
			request.departureStationId(), request.arrivalStationId());

		return trainCarService.getTrainCarSeatDetail(request);
	}

	// ===== Private Helper Methods =====

	/**
	 * 승객 수에 맞는 추천 객차 선택
	 * TODO: 조금 더 고도화된 객차 추천 알고리즘 필요
	 */
	private String selectRecommendedCar(List<TrainCarInfo> availableCars, int passengerCount) {
		// 승객 수보다 잔여 좌석이 많은 객차 필터링
		List<TrainCarInfo> suitableCars = availableCars.stream()
			.filter(car -> car.remainingSeats() >= passengerCount)
			.toList();

		// 적합한 객차가 있으면 중간 위치 선택, 없으면 첫 번째 객차
		if (!suitableCars.isEmpty()) {
			int middleIndex = suitableCars.size() / 2;
			return suitableCars.get(middleIndex).carNumber();
		}

		return availableCars.get(0).carNumber();
	}
}
