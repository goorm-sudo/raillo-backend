package com.sudo.railo.train.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.application.dto.TrainScheduleBasicInfo;
import com.sudo.railo.train.application.dto.request.TrainCarListRequest;
import com.sudo.railo.train.application.dto.response.TrainCarInfo;
import com.sudo.railo.train.application.dto.response.TrainCarListResponse;
import com.sudo.railo.train.exception.TrainErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TrainSearchApplicationService {

	private final TrainScheduleService trainScheduleService;
	private final TrainCarService trainCarService;

	/**
	 * 열차 객차 목록 조회 (잔여 좌석이 있는 객차만)
	 * - TrainScheduleService: 열차 기본 정보 조회
	 * - TrainCarService: 객차 목록 조회
	 */
	public TrainCarListResponse getAvailableTrainCars(TrainCarListRequest request) {
		log.info("열차 객차 목록 조회: trainScheduleId={}, {}역 -> {}역, 승객={}명",
			request.trainScheduleId(), request.departureStationId(),
			request.arrivalStationId(), request.passengerCount());

		// 1. 열차 스케줄 기본 정보 조회
		TrainScheduleBasicInfo scheduleInfo = trainScheduleService.getTrainScheduleBasicInfo(request.trainScheduleId());

		// 2. 잔여 좌석이 있는 객차 목록 조회
		List<TrainCarInfo> availableCars = trainCarService.getAvailableTrainCars(
			request.trainScheduleId(), request.departureStationId(), request.arrivalStationId());

		if (availableCars.isEmpty()) {
			log.warn("잔여 좌석이 있는 객차가 없음: trainScheduleId={}", request.trainScheduleId());
			throw new BusinessException(TrainErrorCode.NO_AVAILABLE_CARS);
		}

		// 3. 승객 수에 맞는 추천 객차 선택 (Application Service 책임)
		String recommendedCarNumber = selectRecommendedCar(availableCars, request.passengerCount());

		log.info("열차 객차 목록 조회 완료: {}개 객차, 추천 객차={}, 열차={}-{}",
			availableCars.size(), recommendedCarNumber,
			scheduleInfo.trainClassificationCode(), scheduleInfo.trainNumber());

		return TrainCarListResponse.of(
			recommendedCarNumber,
			availableCars.size(),
			scheduleInfo.trainClassificationCode(), // TrainScheduleService에서 조회
			scheduleInfo.trainNumber(), // TrainScheduleService에서 조회
			availableCars
		);
	}

	// ===== Private Helper Methods =====

	/**
	 * 승객 수에 맞는 추천 객차 선택
	 * TODO: 조금 더 고도화된 객차 추천 알고리즘 필요
	 */
	private String selectRecommendedCar(List<TrainCarInfo> availableCars, int passengerCount) {
		// 승객 수보다 잔여 좌석이 많은 객차 중에서 중간 위치 선택
		return availableCars.stream()
			.filter(car -> car.remainingSeats() >= passengerCount)
			.skip(availableCars.size() / 2) // 중간 객차 선택
			.findFirst()
			.map(TrainCarInfo::carNumber)
			.orElse(availableCars.get(0).carNumber()); // 없으면 첫 번째 객차
	}
}
