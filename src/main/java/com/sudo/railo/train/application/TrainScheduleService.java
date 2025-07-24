package com.sudo.railo.train.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.util.MonitorPerformance;
import com.sudo.railo.global.util.TrackQuery;
import com.sudo.railo.train.application.dto.SeatReservationInfo;
import com.sudo.railo.train.application.dto.SectionSeatStatus;
import com.sudo.railo.train.application.dto.TrainBasicInfo;
import com.sudo.railo.train.application.dto.TrainScheduleBasicInfo;
import com.sudo.railo.train.application.dto.request.TrainSearchRequest;
import com.sudo.railo.train.application.dto.response.OperationCalendarItem;
import com.sudo.railo.train.application.dto.response.SeatTypeInfo;
import com.sudo.railo.train.application.dto.response.StandingTypeInfo;
import com.sudo.railo.train.application.dto.response.TrainSearchResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchSlicePageResponse;
import com.sudo.railo.train.application.validator.TrainSearchValidator;
import com.sudo.railo.train.domain.StationFare;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.exception.TrainErrorCode;
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

	@Value("${train.standing.ratio:0.15}") // 기본값 0.15
	private double standingRatio;  //TODO: 열차 종류별 다른 % 적용

	private static final double STANDING_FARE_DISCOUNT_RATE = 0.15;
	private static final int MAX_STANDING_CAPACITY = 50;   // 열차별 최대 입석 인원 임시 하드코딩

	private final TrainSearchValidator trainSearchValidator;
	private final TrainScheduleRepository trainScheduleRepository;
	private final TrainScheduleRepositoryCustom trainScheduleRepositoryCustom;
	private final StationFareRepository stationFareRepository;
	private final StationRepository stationRepository;
	private final SeatReservationRepositoryCustom seatReservationRepositoryCustom;

	/**
	 * 운행 캘린더 조회
	 * @return
	 */
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
	@MonitorPerformance(value = "통합 열차 조회", enableN1Detection = true)
	public TrainSearchSlicePageResponse searchTrains(TrainSearchRequest request, Pageable pageable) {
		// request 검증 (route, operationDate, departureTime)
		trainSearchValidator.validateTrainSearchRequest(request);

		// 1.  조회 조건에 맞는 기본 열차 정보 조회
		Slice<TrainBasicInfo> trainInfoSlice = findTrainBasicInfo(request, pageable);

		// 2. 빈 결과 처리 - 정상 응답으로 반환
		if (trainInfoSlice.isEmpty()) {
			log.info("열차 조회 결과 없음: {}역 -> {}역, {}, {}시 이후 - 빈 결과 반환",
				request.departureStationId(), request.arrivalStationId(),
				request.operationDate(), request.departureHour());
			return TrainSearchSlicePageResponse.empty(pageable);
		}

		// 3. 구간별 요금 정보 조회 (일반실/특실 요금)
		StationFare fare = findStationFare(request.departureStationId(), request.arrivalStationId());

		// 4. 각 열차별 좌석 상태 계산 및 응답 생성
		List<TrainSearchResponse> trainSearchResults = processTrainSearchResults(trainInfoSlice.getContent(), fare,
			request);

		log.info("Slice 기반 열차 조회 완료: {}건 조회, hasNext: {}", trainSearchResults.size(), trainInfoSlice.hasNext());

		return createTrainSearchPageResponse(trainSearchResults, trainInfoSlice);
	}

	/**
	 * 개별 열차 스케줄 기본 정보 조회
	 */
	public TrainScheduleBasicInfo getTrainScheduleBasicInfo(Long trainScheduleId) {
		TrainSchedule trainSchedule = trainScheduleRepository.findById(trainScheduleId)
			.orElseThrow(() -> new BusinessException(TrainErrorCode.TRAIN_SCHEDULE_DETAIL_NOT_FOUND));

		return TrainScheduleBasicInfo.of(
			trainSchedule.getId(),
			trainSchedule.getTrain().getTrainType().getDescription(),
			String.format("%03d", trainSchedule.getTrain().getTrainNumber()),
			trainSchedule.getTrain().getTrainName()
		);
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
	@TrackQuery(queryName = "findStationFare")
	private StationFare findStationFare(Long departureStationId, Long arrivalStationId) {
		log.debug("요금 정보 조회: {} -> {}", departureStationId, arrivalStationId);
		return stationFareRepository.findByDepartureStationIdAndArrivalStationId(departureStationId, arrivalStationId)
			.orElseThrow(() -> {
				log.error("요금 정보 없음: {} -> {}", departureStationId, arrivalStationId);
				return new BusinessException(TrainErrorCode.STATION_FARE_NOT_FOUND);
			});
	}

	/**
	 * 열차 조회 결과 일괄 처리 (배치 쿼리 사용)
	 * 모든 열차의 데이터를 배치로 조회한 후 개별 처리
	 */
	private List<TrainSearchResponse> processTrainSearchResults(List<TrainBasicInfo> trainInfoSlice,
		StationFare fare, TrainSearchRequest request) {

		if (trainInfoSlice.isEmpty()) {
			return List.of();
		}

		// 1. trainScheduleId 리스트 추출
		List<Long> trainScheduleIds = trainInfoSlice.stream()
			.map(TrainBasicInfo::trainScheduleId)
			.toList();

		log.info("배치 쿼리 시작: {}건의 열차 일괄 처리", trainScheduleIds.size());

		// 2. 배치 쿼리로 모든 데이터 한번에 조회
		// 2-1. 겹치는 예약 조회
		Map<Long, List<SeatReservationInfo>> overlappingReservationsMap =
			seatReservationRepositoryCustom.findOverlappingReservationsBatch(
				trainScheduleIds, request.departureStationId(), request.arrivalStationId());

		// 2-2. 객차 타입별 좌석 수 조회
		Map<Long, Map<CarType, Integer>> totalSeatsByCarTypeMap =
			trainScheduleRepositoryCustom.findTotalSeatsByCarTypeBatch(trainScheduleIds);

		// 2-3. 열차의 전체 좌석 수 조회
		Map<Long, Integer> totalSeatsMap =
			trainScheduleRepositoryCustom.findTotalSeatsByTrainScheduleIdBatch(trainScheduleIds);

		// 2-4. 열차의 입석 예약 수 조회
		Map<Long, Integer> standingReservationsMap =
			seatReservationRepositoryCustom.countOverlappingStandingReservationsBatch(
				trainScheduleIds, request.departureStationId(), request.arrivalStationId());

		log.info("배치 쿼리 완료: 예약정보={}, 좌석정보={}, 전체좌석={}, 입석={}",
			overlappingReservationsMap.size(), totalSeatsByCarTypeMap.size(),
			totalSeatsMap.size(), standingReservationsMap.size());

		// 3. 각 열차별로 배치 조회된 데이터를 사용해 응답 생성
		List<TrainSearchResponse> results = trainInfoSlice.stream()
			.map(trainInfo -> {
				try {
					Long trainScheduleId = trainInfo.trainScheduleId();

					List<SeatReservationInfo> overlappingReservations =
						overlappingReservationsMap.getOrDefault(trainScheduleId, List.of());
					Map<CarType, Integer> totalSeats =
						totalSeatsByCarTypeMap.getOrDefault(trainScheduleId, Map.of());
					Integer totalSeatCount = totalSeatsMap.getOrDefault(trainScheduleId, 0);
					Integer standingReservations = standingReservationsMap.getOrDefault(trainScheduleId, 0);

					// 좌석 상태 계산 (일반실, 특실, 입석)
					SectionSeatStatus sectionStatus = calculateSectionSeatStatusWithBatchData(
						overlappingReservations, totalSeats, totalSeatCount,
						standingReservations, request.passengerCount());

					return createTrainSearchResponse(trainInfo, sectionStatus, fare, request.passengerCount());
				} catch (Exception e) {
					log.warn("열차 {} 처리 실패: {}", trainInfo.trainNumber(), e.getMessage());
					return null;
				}
			})
			.filter(Objects::nonNull)
			.toList();

		if (results.isEmpty()) {
			log.warn("처리 가능한 열차 없음");
			throw new BusinessException(TrainErrorCode.NO_SEARCH_RESULTS);
		}

		log.info("배치 처리 완료: 전체 {}건 중 {}건 성공", trainInfoSlice.size(), results.size());
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
	 * 열차 조회 응답 생성
	 * @param trainInfo 기본 열차 정보
	 * @param sectionStatus 좌석 상태 종합 정보 (일반실/특실/입석)
	 * @param fare 구간 요금 정보
	 * @param passengerCount 요청 승객 수
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
			trainInfo.trainScheduleId(),
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
		// 입석 가능 열차 && 일반실 예약 불가(매진)
		boolean shouldShowStanding = sectionStatus.standingAvailable() && !sectionStatus.canReserveStandard();

		if (shouldShowStanding) {
			int standingFare = (int)(fare.getStandardFare() * (1.0 - STANDING_FARE_DISCOUNT_RATE));
			return StandingTypeInfo.create(
				sectionStatus.maxAdditionalStanding(),
				MAX_STANDING_CAPACITY,
				standingFare
			);
		}
		return null;
	}

	/**
	 * 배치로 조회된 데이터를 사용해 좌석 상태 계산
	 */
	private SectionSeatStatus calculateSectionSeatStatusWithBatchData(
		List<SeatReservationInfo> overlappingReservations,
		Map<CarType, Integer> totalSeats,
		Integer totalSeatCount,
		Integer standingReservations,
		int requestedPassengerCount) {

		// 1. 좌석 계산
		SeatCalculationResult seatResult = calculateAvailableSeats(totalSeats, overlappingReservations);

		// 2. 입석 계산
		int maxAllowedStandingCount = (int)(totalSeatCount * standingRatio);
		int maxAdditionalStanding = Math.max(0, maxAllowedStandingCount - standingReservations);

		StandingCalculationResult standingResult = new StandingCalculationResult(
			maxAdditionalStanding > 0,   // 입석 가능 열차 판단
			maxAdditionalStanding,
			maxAdditionalStanding >= requestedPassengerCount,
			standingReservations
		);

		// 3. 예약 가능 여부 판단
		boolean canReserveStandard = seatResult.standardAvailable() >= requestedPassengerCount;
		boolean canReserveFirstClass = seatResult.firstClassAvailable() >= requestedPassengerCount;

		return SectionSeatStatus.of(
			seatResult.standardAvailable(), seatResult.standardTotal(),
			seatResult.firstClassAvailable(), seatResult.firstClassTotal(),
			canReserveStandard, canReserveFirstClass,
			standingResult.standingAvailable(), standingResult.maxAdditionalStanding(),
			standingResult.canReserveStanding(), standingResult.currentStandingReservations()
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

			int maxAllowedStandingCount = (int)(totalSeats * standingRatio);

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
		boolean canReserveStanding, int currentStandingReservations
	) {
	}
}
