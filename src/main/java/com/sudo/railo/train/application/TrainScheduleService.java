package com.sudo.railo.train.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
import com.sudo.railo.train.application.dto.response.TrainSearchResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchSlicePageResponse;
import com.sudo.railo.train.domain.StationFare;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.exception.TrainErrorCode;
import com.sudo.railo.train.infrastructure.ScheduleStopRepository;
import com.sudo.railo.train.infrastructure.SeatReservationRepositoryCustom;
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

	private static final double STANDING_RATIO = 0.15; // 15%, TODO: 열차 종류별 다른 % 적용

	private final TrainScheduleRepository trainScheduleRepository;
	private final TrainScheduleRepositoryCustom trainScheduleRepositoryCustom;
	private final StationFareRepository stationFareRepository;
	private final ScheduleStopRepository scheduleStopRepository;
	private final StationRepository stationRepository;
	private final SeatReservationRepositoryCustom seatReservationRepositoryCustom;

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
	 * 통합 열차 조회 (메인 검색)
	 * 1. 조회 조건으로 기본 열차 정보 조회 (페이징)
	 * 2. 구간별 요금 정보 조회
	 * 3. 각 열차별 좌석 상태 계산 및 응답 생성
	 * 4. 최종 조회 결과 반환
	 */
	public TrainSearchSlicePageResponse searchTrains(TrainSearchRequest request, Pageable pageable) {
		log.info("열차 조회 시작: {} -> {}, {}, 승객: {}명, 출발 시간: {}시 이후",
			request.departureStationId(), request.arrivalStationId(),
			request.operationDate(), request.passengerCount(), request.departureHour());

		// 1.  조회 조건에 맞는 기본 열차 정보 조회
		Slice<TrainBasicInfo> trainSlice = findTrainBasicInfo(request, pageable);

		// 2. 빈 결과 처리 - 정상 응답으로 반환
		if (trainSlice.isEmpty()) {
			log.info("열차 조회 결과 없음: {}역 -> {}역, {}, {}시 이후 - 빈 결과 반환",
				request.departureStationId(), request.arrivalStationId(),
				request.operationDate(), request.departureHour());
			return TrainSearchSlicePageResponse.empty(pageable);
		}

		// 3. 구간별 요금 정보 조회 (일반실/특실 요금)
		StationFare fare = findStationFare(request.departureStationId(), request.arrivalStationId());

		// 4. 각 열차별 좌석 상태 계산 및 응답 생성
		List<TrainSearchResponse> trainSearchResults = processTrainSearchResults(trainSlice.getContent(), fare,
			request);

		log.info("Slice 기반 열차 조회 완료: {}건 조회, hasNext: {}", trainSearchResults.size(), trainSlice.hasNext());

		return createTrainSearchPageResponse(trainSearchResults, trainSlice);
	}

	// ============================================
	// 메인 열차 조회 플로우
	// ============================================

	/**
	 * 기본 열차 정보 조회
	 */
	private Slice<TrainBasicInfo> findTrainBasicInfo(TrainSearchRequest request, Pageable pageable) {
		LocalTime departureTimeFrom = request.getDepartureTimeFilter();

		Slice<TrainBasicInfo> trainPage = trainScheduleRepositoryCustom.findTrainBasicInfo(
			request.departureStationId(),
			request.arrivalStationId(),
			request.operationDate(),
			departureTimeFrom,
			pageable);

		return trainPage;
	}

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
	 * 열차 조회 결과 일괄 처리 (각 열차별로 좌석 상태 계산)
	 * @param trainInfos 기본 열차 정보 리스트
	 * @param fare 구간 요금 정보
	 * @param request 조회 요청 정보
	 * @return 좌석 상태가 포함된 열차 조회 결과
	 */
	private List<TrainSearchResponse> processTrainSearchResults(List<TrainBasicInfo> trainInfos, StationFare fare,
		TrainSearchRequest request) {

		List<TrainSearchResponse> results = trainInfos.stream()
			.map(trainInfo -> processIndividualTrain(trainInfo, fare, request)) // 각 개별 열차 처리
			.filter(Objects::nonNull) // 처리 실패한 열차 제외
			.toList();

		if (results.isEmpty()) {
			log.warn("처리 가능한 열차 없음");
			throw new BusinessException(TrainErrorCode.NO_SEARCH_RESULTS);
		}

		log.info("열차 조회 완료: 전체 {}건 중 {}건 처리 성공", trainInfos.size(), results.size());
		return results;
	}

	/**
	 * 열차 조회 응답 생성
	 */
	private TrainSearchSlicePageResponse createTrainSearchPageResponse(List<TrainSearchResponse> trainSearchResults,
		Slice<TrainBasicInfo> trainSlice) {
		return TrainSearchSlicePageResponse.of(trainSearchResults, trainSlice);
	}

	// ============================================
	// 개별 열차 처리 메소드
	// ============================================

	/**
	 * 개별 열차 처리 (좌석 상태 계산 + 응답 생성)
	 * @param trainInfo 기본 열차 정보
	 * @param fare 구간 요금 정보
	 * @param request 조회 요청 정보
	 * @return 처리된 열차 조회 응답 (실패시 null)
	 */
	private TrainSearchResponse processIndividualTrain(TrainBasicInfo trainInfo, StationFare fare,
		TrainSearchRequest request) {
		try {
			SectionSeatStatus sectionStatus = calculateSectionSeatStatus(
				trainInfo.trainScheduleId(),
				request.departureStationId(),
				request.arrivalStationId(),
				request.passengerCount());

			return createTrainSearchResponse(trainInfo, sectionStatus, fare, request.passengerCount());
		} catch (Exception e) {
			log.warn("열차 {} 처리 실패: {}", trainInfo.trainNumber(), e.getMessage());
			return null;
		}
	}

	/**
	 * 열차 조회 응답 생성
	 * @param trainInfo 기본 열차 정보
	 * @param sectionStatus 계산된 좌석 상태
	 * @param fare 구간 요금 정보
	 * @param passengerCount 승객 수
	 * @return 완성된 열차 조회 응답
	 */
	private TrainSearchResponse createTrainSearchResponse(TrainBasicInfo trainInfo, SectionSeatStatus sectionStatus,
		StationFare fare, int passengerCount) {

		boolean hasStanding = sectionStatus.standingAvailable();

		// 1. 좌석 타입별 정보 생성 (일반실 / 특실)
		SeatTypeInfo standardSeatInfo = SeatTypeInfo.create(
			sectionStatus.standardAvailable(),
			sectionStatus.standardTotal(),
			fare.getStandardFare(),
			passengerCount,
			"일반실",
			hasStanding
		);
		SeatTypeInfo firstClassSeatInfo = SeatTypeInfo.create(
			sectionStatus.firstClassAvailable(),
			sectionStatus.firstClassTotal(),
			fare.getFirstClassFare(),
			passengerCount,
			"특실",
			false
		);

		// 2. 입석 정보 생성
		StandingTypeInfo standingInfo = createStandingInfoIfNeeded(sectionStatus, fare);

		return TrainSearchResponse.of(
			String.format("%03d", trainInfo.trainNumber()),  // 열차번호 3자리 포맷
			trainInfo.trainName(),                           // 열차명
			trainInfo.departureStationName(),                // 출발역명
			trainInfo.arrivalStationName(),                  // 도착역명
			trainInfo.departureTime(),                       // 출발시간
			trainInfo.arrivalTime(),                         // 도착시간
			standardSeatInfo,                               // 일반실 정보
			firstClassSeatInfo,                             // 특실 정보
			standingInfo                                    // 입석 정보 (있으면 포함, 없으면 null)
		);
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

	// ============================================
	// 좌석 계산 및 상태 판정 메소드
	// ============================================

	/**
	 * 구간별 좌석 상태 종합 계산
	 */
	private SectionSeatStatus calculateSectionSeatStatus(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId, int passengerCount) {

		// 겹치는 예약 정보 조회
		List<SeatReservationInfo> overlappingReservations = seatReservationRepositoryCustom.findOverlappingReservations(
			trainScheduleId, departureStationId, arrivalStationId);

		// 열차 별 좌석 수 조회
		Map<CarType, Integer> totalSeats = trainScheduleRepositoryCustom.findTotalSeatsByCarType(trainScheduleId);

		// 좌석 계산 (일반 좌석, 입석)
		SeatCalculationResult seatResult = calculateAvailableSeats(totalSeats, overlappingReservations);
		StandingCalculationResult standingResult = calculateStandingAvailability(trainScheduleId, departureStationId,
			arrivalStationId, passengerCount);

		boolean canReserveStandard = seatResult.standardAvailable() >= passengerCount;
		boolean canReserveFirstClass = seatResult.firstClassAvailable() >= passengerCount;
		boolean canReserveStanding = standingResult.canReserveStanding();

		return SectionSeatStatus.of(
			seatResult.standardAvailable(), seatResult.standardTotal(),
			seatResult.firstClassAvailable(), seatResult.firstClassTotal(),
			canReserveStandard, canReserveFirstClass,
			standingResult.standingAvailable(), standingResult.maxAdditionalStanding(),
			canReserveStanding, standingResult.maxOccupancyInRoute()
		);
	}

	/**
	 * 좌석 타입별 잔여 좌석 계산
	 */
	private SeatCalculationResult calculateAvailableSeats(Map<CarType, Integer> totalSeats,
		List<SeatReservationInfo> overlappingReservations) {
		// 총 좌석 수
		int standardTotal = totalSeats.getOrDefault(CarType.STANDARD, 0);
		int firstClassTotal = totalSeats.getOrDefault(CarType.FIRST_CLASS, 0);

		// 예약된 좌석 수 계산
		Map<CarType, Long> occupiedSeats = overlappingReservations.stream()
			.collect(Collectors.groupingBy(SeatReservationInfo::carType, Collectors.counting()));

		int standardOccupied = occupiedSeats.getOrDefault(CarType.STANDARD, 0L).intValue();
		int firstClassOccupied = occupiedSeats.getOrDefault(CarType.FIRST_CLASS, 0L).intValue();

		return new SeatCalculationResult(
			Math.max(0, standardTotal - standardOccupied), standardTotal,
			Math.max(0, firstClassTotal - firstClassOccupied), firstClassTotal
		);
	}

	/**
	 * 입석 가능 여부 및 수량 계산
	 */
	private StandingCalculationResult calculateStandingAvailability(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId, int passengerCount) {
		try {
			int totalSeats = trainScheduleRepositoryCustom.findTotalSeatsByTrainScheduleId(trainScheduleId);

			int maxAllowedStandingCount = (int)(totalSeats * STANDING_RATIO);

			// 현재 구간에서 예약된 입석 수 조회, 추가 입석 가능 인원 수 계산
			int currentStandingReservations = seatReservationRepositoryCustom.countOverlappingStandingReservations(
				trainScheduleId, departureStationId, arrivalStationId);
			int maxAdditionalStanding = Math.max(0, maxAllowedStandingCount - currentStandingReservations);

			boolean standingAvailable = maxAdditionalStanding > 0;
			boolean canReserveStanding = maxAdditionalStanding >= passengerCount;

			log.debug("입석 계산 완료: 총허용={}, 현재예약={}, 추가가능={}, 예약가능={}",
				maxAllowedStandingCount, currentStandingReservations, maxAdditionalStanding, canReserveStanding);

			return new StandingCalculationResult(standingAvailable, maxAdditionalStanding, canReserveStanding,
				currentStandingReservations);
		} catch (Exception e) {
			log.warn("입석 계산 중 오류: trainScheduleId={}", trainScheduleId, e);
			return new StandingCalculationResult(false, 0, false, 0);
		}
	}

	// ============================================
	// Service Layer 전용 내부 Records
	// ============================================

	private record SeatCalculationResult(
		int standardAvailable, int standardTotal,
		int firstClassAvailable, int firstClassTotal
	) {
	}

	private record StandingCalculationResult(
		boolean standingAvailable, int maxAdditionalStanding,
		boolean canReserveStanding, int maxOccupancyInRoute
	) {
	}
}
