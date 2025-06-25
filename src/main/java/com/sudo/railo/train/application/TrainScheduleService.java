package com.sudo.railo.train.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.application.dto.SeatReservationInfo;
import com.sudo.railo.train.application.dto.SectionSeatStatus;
import com.sudo.railo.train.application.dto.TrainBasicInfo;
import com.sudo.railo.train.application.dto.request.OperationCalendarItem;
import com.sudo.railo.train.application.dto.request.TrainSearchRequest;
import com.sudo.railo.train.application.dto.response.SeatTypeInfo;
import com.sudo.railo.train.application.dto.response.StandingTypeInfo;
import com.sudo.railo.train.application.dto.response.TrainSearchPageResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchResponse;
import com.sudo.railo.train.domain.StationFare;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.domain.type.SeatAvailabilityStatus;
import com.sudo.railo.train.exception.TrainErrorCode;
import com.sudo.railo.train.infrastructure.ScheduleStopRepository;
import com.sudo.railo.train.infrastructure.SeatReservationRepository;
import com.sudo.railo.train.infrastructure.StationFareRepository;
import com.sudo.railo.train.infrastructure.StationRepository;
import com.sudo.railo.train.infrastructure.TrainScheduleRepository;
import com.sudo.railo.train.infrastructure.TrainScheduleRepositoryCustom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TrainScheduleService {

	private final TrainScheduleRepository trainScheduleRepository;
	private final TrainScheduleRepositoryCustom trainScheduleRepositoryCustom;
	private final SeatReservationRepository seatReservationRepository;
	private final StationFareRepository stationFareRepository;
	private final ScheduleStopRepository scheduleStopRepository;
	private final StationRepository stationRepository;

	public List<OperationCalendarItem> getOperationCalendar() {
		LocalDate startDate = LocalDate.now();
		LocalDate endDate = startDate.plusMonths(1);

		// 운행 날짜 조회 (Set으로 반환)
		Set<LocalDate> datesWithSchedule = trainScheduleRepositoryCustom.findDatesWithActiveSchedules(startDate,
			endDate);

		List<OperationCalendarItem> calendar = startDate.datesUntil(endDate.plusDays(1))
			.map(date -> {
				boolean isHoliday = isHoliday(date);
				boolean hasSchedule = datesWithSchedule.contains(date);
				return OperationCalendarItem.create(date, isHoliday, hasSchedule);
			})
			.toList();

		log.info("운행 캘린더 조회 완료: {} ~ {} ({} 일), 운행일수: {}",
			startDate, endDate, calendar.size(), datesWithSchedule.size());

		return calendar;
	}

	/**
	 * 휴일 여부 판단
	 * TODO : 공휴일 API 연동 후 판단 처리 로직 추가 필요
	 */
	private boolean isHoliday(LocalDate date) {
		// 2025년 공휴일 임시 하드코딩
		Set<LocalDate> holidays2025 = Set.of(
			LocalDate.of(2025, 1, 1),   // 신정
			LocalDate.of(2025, 1, 28),  // 설날 연휴
			LocalDate.of(2025, 1, 29),  // 설날
			LocalDate.of(2025, 1, 30),  // 설날 연휴
			LocalDate.of(2025, 3, 1),   // 삼일절
			LocalDate.of(2025, 5, 5),   // 어린이날
			LocalDate.of(2025, 6, 6),   // 현충일
			LocalDate.of(2025, 8, 15),  // 광복절
			LocalDate.of(2025, 10, 3),  // 개천절
			LocalDate.of(2025, 10, 5),  // 추석 연휴
			LocalDate.of(2025, 10, 6),  // 추석 연휴
			LocalDate.of(2025, 10, 7),  // 추석 연휴
			LocalDate.of(2025, 10, 8),  // 추석
			LocalDate.of(2025, 10, 9),  // 한글날
			LocalDate.of(2025, 12, 25)  // 크리스마스
		);

		return holidays2025.contains(date);
	}

	/**
	 * 통합 열차 검색 (메인 검색)
	 */
	public TrainSearchPageResponse searchTrains(TrainSearchRequest request, Pageable pageable) {
		log.info("열차 검색 시작: {} -> {}, {}, 승객: {}명, 출발 시간: {}시 이후",
			request.departureStationId(), request.arrivalStationId(),
			request.operationDate(), request.passengerCount(), request.departureHour());

		try {
			LocalTime departureTimeFrom = request.getDepartureTimeFilter();

			Page<TrainBasicInfo> trainPage = trainScheduleRepositoryCustom.findTrainBasicInfo(
				request.departureStationId(), request.arrivalStationId(), request.operationDate(), departureTimeFrom,
				pageable);

			if (trainPage.isEmpty()) {
				log.warn("열차 조회 결과 없음: {}역 -> {}역, {}, {}시 이후",
					request.departureStationId(), request.arrivalStationId(), request.operationDate(),
					request.departureHour());
				throw new BusinessException(TrainErrorCode.NO_OPERATION_ON_DATE,
					String.format("%s %시 이후 운행하는 열차가 없습니다.", request.operationDate(), request.departureHour()));
			}

			StationFare fare = findStationFare(request.departureStationId(), request.arrivalStationId());
			List<TrainSearchResponse> trainSearchResults = processTrainSearchResults(trainPage.getContent(), fare,
				request);

			if (trainSearchResults.isEmpty()) {
				log.warn("처리 가능한 열차 없음");
				throw new BusinessException(TrainErrorCode.NO_SEARCH_RESULTS);
			}

			log.info("열차 조회 완료: 전체 {}건 중 {}건 처리 성공",
				trainPage.getContent().size(), trainSearchResults.size());

			return TrainSearchPageResponse.of(trainSearchResults, trainPage.getNumber(), trainPage.getSize(),
				trainPage.getTotalElements(), trainPage.getTotalPages(), trainPage.hasNext(), trainPage.hasPrevious());
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("열차 검색 중 시스템 오류 발생", e);
			throw new BusinessException(TrainErrorCode.TRAIN_SYSTEM_ERROR, e);
		}
	}

	// ============================================
	// Private Helper Methods
	// ============================================

	/**
	 * 구간별 요금 정보 조회
	 */
	private StationFare findStationFare(Long departureStationId, Long arrivalStationId) {
		log.debug("요금 정보 조회: {} -> {}", departureStationId, arrivalStationId);
		return stationFareRepository.findByDepartureStationIdAndArrivalStationId(departureStationId, arrivalStationId)
			.orElseThrow(() -> {
				log.error("요금 정보 없음: {} -> {}", departureStationId, arrivalStationId);
				return new BusinessException(TrainErrorCode.STATION_FARE_NOT_FOUND);
			});
	}

	/**
	 * 열차 검색 결과 일괄 처리
	 */
	private List<TrainSearchResponse> processTrainSearchResults(List<TrainBasicInfo> trainInfos, StationFare fare,
		TrainSearchRequest request) {
		return trainInfos.stream()
			.map(trainInfo -> processIndividualTrain(trainInfo, fare, request))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	/**
	 * 개별 열차 처리
	 */
	private TrainSearchResponse processIndividualTrain(TrainBasicInfo trainInfo, StationFare fare,
		TrainSearchRequest request) {
		try {
			SectionSeatStatus sectionStatus = calculateSectionSeatStatus(
				trainInfo.trainScheduleId(), request.departureStationId(), request.arrivalStationId(),
				request.passengerCount());
			return createTrainSearchResponse(trainInfo, sectionStatus, fare, request.passengerCount());
		} catch (Exception e) {
			log.warn("열차 {} 처리 실패: {}", trainInfo.trainNumber(), e.getMessage());
			return null;
		}
	}

	/**
	 * 열차 검색 응답 생성
	 */
	private TrainSearchResponse createTrainSearchResponse(TrainBasicInfo trainInfo, SectionSeatStatus sectionStatus,
		StationFare fare, int passengerCount) {
		try {
			SeatTypeInfo standardSeatInfo = SeatTypeInfo.create(sectionStatus.standardAvailable(),
				sectionStatus.standardTotal(), fare.getStandardFare(), passengerCount, "일반실");
			SeatTypeInfo firstClassSeatInfo = SeatTypeInfo.create(sectionStatus.firstClassAvailable(),
				sectionStatus.firstClassTotal(), fare.getFirstClassFare(), passengerCount, "특실");
			StandingTypeInfo standingInfo = createStandingInfoIfNeeded(sectionStatus, fare);
			SeatAvailabilityStatus overallStatus = determineOverallStatus(sectionStatus);

			if (standingInfo != null) {
				return TrainSearchResponse.withStanding(
					String.format("%03d", trainInfo.trainNumber()),
					trainInfo.trainName(),
					trainInfo.departureTime(),
					trainInfo.arrivalTime(),
					standardSeatInfo,
					firstClassSeatInfo,
					standingInfo,
					overallStatus
				);
			} else {
				return TrainSearchResponse.seatsOnly(
					String.format("%03d", trainInfo.trainNumber()),
					trainInfo.trainName(),
					trainInfo.departureTime(),
					trainInfo.arrivalTime(),
					standardSeatInfo,
					firstClassSeatInfo,
					overallStatus
				);
			}
		} catch (Exception e) {
			log.error("열차 응답 생성 중 오류: 열차번호={}", trainInfo.trainNumber(), e);
			throw new BusinessException(TrainErrorCode.TRAIN_SYSTEM_ERROR,
				"열차 정보 생성 중 오류: " + trainInfo.trainNumber());
		}
	}

	/**
	 * 입석 정보 생성 (필요한 경우만)
	 */
	private StandingTypeInfo createStandingInfoIfNeeded(SectionSeatStatus sectionStatus, StationFare fare) {
		boolean shouldShowStanding = sectionStatus.standingAvailable() &&
			(!sectionStatus.canReserveStandard() || !sectionStatus.canReserveFirstClass());

		if (shouldShowStanding) {
			int standingFare = (int)(fare.getStandardFare() * 0.9);
			return StandingTypeInfo.create(sectionStatus.maxAdditionalStanding(), 50, standingFare);
		}
		return null;
	}

	/**
	 * 구간별 좌석 상태 종합 계산
	 */
	private SectionSeatStatus calculateSectionSeatStatus(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId, int passengerCount) {
		try {
			List<SeatReservationInfo> overlappingReservations = seatReservationRepository.findOverlappingReservations(
				trainScheduleId, departureStationId, arrivalStationId);
			Map<CarType, Integer> totalSeats = trainScheduleRepositoryCustom.findTotalSeatsByCarType(trainScheduleId);

			SeatCalculationResult seatResult = calculateAvailableSeats(totalSeats, overlappingReservations);
			StandingCalculationResult standingResult = calculateStandingAvailability(trainScheduleId,
				departureStationId, arrivalStationId, passengerCount);

			boolean canReserveStandard = seatResult.standardAvailable() >= passengerCount;
			boolean canReserveFirstClass = seatResult.firstClassAvailable() >= passengerCount;
			boolean canReserveStanding = standingResult.canReserveStanding();

			return SectionSeatStatus.of(seatResult.standardAvailable(), seatResult.standardTotal(),
				seatResult.firstClassAvailable(), seatResult.firstClassTotal(),
				canReserveStandard, canReserveFirstClass,
				standingResult.standingAvailable(), standingResult.maxAdditionalStanding(),
				canReserveStanding, standingResult.maxOccupancyInRoute());
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("좌석 상태 계산 중 오류: trainScheduleId={}", trainScheduleId, e);
			throw new BusinessException(TrainErrorCode.TRAIN_SYSTEM_ERROR, "좌석 상태 계산 중 오류가 발생했습니다.");
		}
	}

	/**
	 * 좌석 타입별 잔여 좌석 계산
	 */
	private SeatCalculationResult calculateAvailableSeats(Map<CarType, Integer> totalSeats,
		List<SeatReservationInfo> overlappingReservations) {
		int standardTotal = totalSeats.getOrDefault(CarType.STANDARD, 0);
		int firstClassTotal = totalSeats.getOrDefault(CarType.FIRST_CLASS, 0);

		Map<CarType, Long> occupiedSeats = overlappingReservations.stream()
			.collect(Collectors.groupingBy(SeatReservationInfo::carType, Collectors.counting()));

		int standardOccupied = occupiedSeats.getOrDefault(CarType.STANDARD, 0L).intValue();
		int firstClassOccupied = occupiedSeats.getOrDefault(CarType.FIRST_CLASS, 0L).intValue();

		return new SeatCalculationResult(Math.max(0, standardTotal - standardOccupied), standardTotal,
			Math.max(0, firstClassTotal - firstClassOccupied), firstClassTotal);
	}

	/**
	 * 입석 가능 여부 및 수량 계산
	 */
	private StandingCalculationResult calculateStandingAvailability(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId, int passengerCount) {
		try {
			int maxTrainCapacity = trainScheduleRepositoryCustom.findMaxCapacityByTrainScheduleId(trainScheduleId);

			if (maxTrainCapacity == 0) {
				log.warn("열차 정보 없음: trainScheduleId={}", trainScheduleId);
				return new StandingCalculationResult(false, 0, false, 0);
			}

			// 간단한 입석 계산 로직
			boolean standingAvailable = true;
			int maxAdditionalStanding = 50; // 임시값
			boolean canReserveStanding = maxAdditionalStanding >= passengerCount;

			return new StandingCalculationResult(standingAvailable, maxAdditionalStanding, canReserveStanding, 0);
		} catch (Exception e) {
			log.warn("입석 계산 중 오류: trainScheduleId={}", trainScheduleId, e);
			return new StandingCalculationResult(false, 0, false, 0);
		}
	}

	/**
	 * 전체 열차 상태 결정
	 */
	private SeatAvailabilityStatus determineOverallStatus(SectionSeatStatus sectionStatus) {
		if (!sectionStatus.canReserveStandard() && !sectionStatus.canReserveFirstClass()
			&& !sectionStatus.canReserveStanding()) {
			return SeatAvailabilityStatus.SOLD_OUT;
		}
		if ((!sectionStatus.canReserveStandard() && !sectionStatus.canReserveFirstClass())
			&& sectionStatus.canReserveStanding()) {
			return SeatAvailabilityStatus.STANDING_AVAILABLE;
		}
		if (!sectionStatus.canReserveStandard() && sectionStatus.canReserveFirstClass()) {
			return SeatAvailabilityStatus.FIRST_CLASS_ONLY;
		}
		if (sectionStatus.standardAvailable() >= 11)
			return SeatAvailabilityStatus.AVAILABLE;
		else if (sectionStatus.standardAvailable() >= 6)
			return SeatAvailabilityStatus.LIMITED;
		else if (sectionStatus.standardAvailable() >= 1)
			return SeatAvailabilityStatus.FEW_REMAINING;
		else
			return SeatAvailabilityStatus.SOLD_OUT;
	}

	// ============================================
	// Service Layer 전용 내부 Records
	// ============================================

	private record SeatCalculationResult(int standardAvailable, int standardTotal, int firstClassAvailable,
										 int firstClassTotal) {
	}

	private record StandingCalculationResult(boolean standingAvailable, int maxAdditionalStanding,
											 boolean canReserveStanding, int maxOccupancyInRoute) {
	}
}
