package com.sudo.railo.train.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.application.dto.projection.SeatProjection;
import com.sudo.railo.train.application.dto.request.TrainCarSeatDetailRequest;
import com.sudo.railo.train.application.dto.response.SeatDetail;
import com.sudo.railo.train.application.dto.response.TrainCarInfo;
import com.sudo.railo.train.application.dto.response.TrainCarSeatDetailResponse;
import com.sudo.railo.train.exception.TrainErrorCode;
import com.sudo.railo.train.infrastructure.SeatRepositoryCustom;
import com.sudo.railo.train.infrastructure.StationRepository;
import com.sudo.railo.train.infrastructure.TrainCarQueryRepositoryCustom;
import com.sudo.railo.train.infrastructure.TrainCarRepository;
import com.sudo.railo.train.infrastructure.TrainScheduleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TrainSeatQueryService {

	private final TrainCarQueryRepositoryCustom trainCarQueryRepositoryCustom;
	private final SeatRepositoryCustom seatRepositoryCustom;

	private final TrainScheduleRepository trainScheduleRepository;
	private final TrainCarRepository trainCarRepository;
	private final StationRepository stationRepository;

	/**
	 * 열차 객차 목록 조회 (잔여 좌석이 있는 객차만)
	 */
	public List<TrainCarInfo> getAvailableTrainCars(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId) {

		validateTrainScheduleExists(trainScheduleId);
		validateStationsExist(departureStationId, arrivalStationId);
		validateRouteDifferent(departureStationId, arrivalStationId);

		// 1. 잔여 좌석이 있는 객차 목록 조회
		List<TrainCarInfo> availableCars = trainCarQueryRepositoryCustom.findAvailableTrainCars(
			trainScheduleId, departureStationId, arrivalStationId);

		if (availableCars.isEmpty()) {
			log.warn("잔여 좌석이 있는 객차가 없음: trainScheduleId={}", trainScheduleId);
			throw new BusinessException(TrainErrorCode.NO_AVAILABLE_CARS);
		}

		return availableCars;
	}

	/**
	 * 열차 객차 좌석 상세 조회
	 */
	public TrainCarSeatDetailResponse getTrainCarSeatDetail(TrainCarSeatDetailRequest request) {

		validateTrainCarExists(request.trainCarId());
		validateTrainScheduleExists(request.trainScheduleId());
		validateStationsExist(request.departureStationId(), request.arrivalStationId());
		validateRouteDifferent(request.departureStationId(), request.arrivalStationId());

		// 1. 객차 좌석 상세 조회
		TrainCarSeatInfo carSeatInfo = seatRepositoryCustom.findTrainCarSeatDetail(
			request.trainCarId(),
			request.trainScheduleId(),
			request.departureStationId(),
			request.arrivalStationId()
		);

		// 2. 좌석 상세 정보 변환
		List<SeatDetail> seatDetails = carSeatInfo.seats().stream()
			.map(this::convertToSeatDetail)
			.toList();

		log.info("열차 객차 좌석 상세 조회 완료: 객차={}, 전체좌석={}, 잔여좌석={}",
			carSeatInfo.carNumber(), carSeatInfo.totalSeats(), carSeatInfo.remainingSeats());

		return TrainCarSeatDetailResponse.of(
			carSeatInfo.carNumber(),
			carSeatInfo.carType(),
			carSeatInfo.totalSeats(),
			carSeatInfo.remainingSeats(),
			carSeatInfo.getLayoutType(),
			seatDetails
		);
	}

	// ===== Validation Methods =====

	private void validateTrainScheduleExists(Long trainScheduleId) {
		if (!trainScheduleRepository.existsById(trainScheduleId)) {
			log.warn("존재하지 않는 열차 스케줄: trainScheduleId={}", trainScheduleId);
			throw new BusinessException(TrainErrorCode.TRAIN_SCHEDULE_NOT_FOUND);
		}
	}

	private void validateTrainCarExists(Long trainCarId) {
		if (!trainCarRepository.existsById(trainCarId)) {
			log.warn("존재하지 않는 객차: trainCarId={}", trainCarId);
			throw new BusinessException(TrainErrorCode.TRAIN_CAR_NOT_FOUND);
		}
	}

	private void validateStationsExist(Long departureStationId, Long arrivalStationId) {
		if (!stationRepository.existsById(departureStationId)) {
			log.warn("존재하지 않는 출발역: departureStationId={}", departureStationId);
			throw new BusinessException(TrainErrorCode.STATION_NOT_FOUND);
		}

		if (!stationRepository.existsById(arrivalStationId)) {
			log.warn("존재하지 않는 도착역: arrivalStationId={}", arrivalStationId);
			throw new BusinessException(TrainErrorCode.STATION_NOT_FOUND);
		}
	}

	private void validateRouteDifferent(Long departureStationId, Long arrivalStationId) {
		if (departureStationId.equals(arrivalStationId)) {
			log.warn("출발역과 도착역이 동일함: stationId={}", departureStationId);
			throw new BusinessException(TrainErrorCode.INVALID_ROUTE);
		}
	}

	// ===== Private Helper Methods =====

	/**
	 * SeatProjection -> SeatDetail 변환
	 */
	private SeatDetail convertToSeatDetail(SeatProjection seatProjection) {
		return SeatDetail.of(
			seatProjection.getSeatId(),
			seatProjection.getSeatNumber(),
			seatProjection.isAvailable(),
			seatProjection.getSeatDirection(),
			seatProjection.getSeatType(),
			seatProjection.getSpecialMessage()
		);
	}
}
