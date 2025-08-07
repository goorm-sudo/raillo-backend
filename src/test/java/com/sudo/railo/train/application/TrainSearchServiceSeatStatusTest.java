package com.sudo.railo.train.application;

import static com.sudo.railo.support.helper.TrainScheduleTestHelper.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.fixture.MemberFixture;
import com.sudo.railo.support.helper.ReservationTestHelper;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.application.dto.request.TrainSearchRequest;
import com.sudo.railo.train.application.dto.response.TrainSearchResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchSlicePageResponse;
import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.domain.type.SeatAvailabilityStatus;
import com.sudo.railo.train.infrastructure.SeatRepository;

import lombok.extern.slf4j.Slf4j;

@ServiceTest
@Slf4j
public class TrainSearchServiceSeatStatusTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TrainSearchService trainSearchService;

	@Autowired
	private ReservationTestHelper reservationTestHelper;

	@Autowired
	private TrainTestHelper trainTestHelper;

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SeatRepository seatRepository;

	record SeatStatusScenario(
		String description,
		int standardCars, int firstClassCars, int standardRows, int firstClassRows,
		int reservedStandardSeats, int reservedFirstClassSeats, int reservedStandingSeats,
		int passengerCount,
		SeatAvailabilityStatus expectedStandardStatus,
		SeatAvailabilityStatus expectedFirstClassStatus,
		boolean expectedStandardCanReserve,
		boolean expectedFirstClassCanReserve,
		boolean expectedHasStanding
	) {
		@Override
		public String toString() {
			return description;
		}
	}

	static Stream<SeatStatusScenario> seatStatusScenarios() {
		return Stream.of(
			new SeatStatusScenario(
				"1. 여유 상황 - 일반실/특실 모두 충분",
				2, 1, 10, 8,    // 일반실 80석, 특실 24석
				5, 2, 0,        // 일반실 5석, 특실 2석 예약 → 75석, 22석 잔여
				4,              // 4명 요청
				// 일반실: 75/80 = 93.7% > 25% → AVAILABLE
				// 특실: 22/24 = 91.6% > 25% → AVAILABLE
				SeatAvailabilityStatus.AVAILABLE, SeatAvailabilityStatus.AVAILABLE,
				true, true, false
			),
			new SeatStatusScenario(
				"2. 제한적 상황 - 일반실 여유 부족하지만 예약 가능",
				2, 1, 10, 8,   // 일반실 80석, 특실 24석
				65, 5, 0,      // 일반실 65석, 특실 5석 예약 → 15석, 19석 잔여
				4,             // 4명 요청
				// 일반실: 15/80 = 18.75% < 25% → LIMITED
				// 특실: 19/24 = 79.1% > 25% → AVAILABLE
				SeatAvailabilityStatus.LIMITED, SeatAvailabilityStatus.AVAILABLE,
				true, true, false
			),
			new SeatStatusScenario(
				"3. 일반실 부족하지만 입석 가능 상황",
				2, 1, 10, 8,   // 일반실 80석, 특실 24석 → 총 104석 → 입석 15석(104*0.15)
				78, 0, 0,      // 일반실 78석 예약 → 2석 잔여, 입석 0석 예약 → 15석 잔여
				5,             // 5명 요청 (일반실 2석 < 5명이므로 예약 불가하지만 입석 15석으로 수용 가능)
				// 일반실: 2 < 5명 요청 → STANDING_ONLY (입석 가능하므로)
				// 특실: 24/24 = 100% > 25% → AVAILABLE
				SeatAvailabilityStatus.STANDING_ONLY, SeatAvailabilityStatus.AVAILABLE,
				false, true, true
			),
			new SeatStatusScenario(
				"4. 일반실 매진하지만 입석 가능 상황",
				2, 1, 10, 8,   // 일반실 80석, 특실 24석 → 총 104석 → 입석 15석
				80, 0, 0,      // 일반실 모두 예약, 입석 0석 예약 → 15석 잔여
				3,             // 3명 요청 (입석 15석으로 수용 가능)
				// 일반실: 0석 잔여, 입석 가능 → STANDING_ONLY
				// 특실: 24/24 = 100% > 25% → AVAILABLE
				SeatAvailabilityStatus.STANDING_ONLY, SeatAvailabilityStatus.AVAILABLE,
				false, true, true
			),
			new SeatStatusScenario(
				"5. 일반실 매진 + 입석 부족 상황",
				2, 1, 10, 8,   // 일반실 80석, 특실 24석 → 총 104석 → 입석 15석
				80, 0, 13,     // 일반실 매진, 입석 13석 예약 → 입석 2석 잔여
				5,             // 5명 요청 (입석 2석으로 수용 불가)
				// 일반실: 0석 잔여, 입석으로도 수용 불가 → SOLD_OUT
				// 특실: 24/24 = 100% > 25% → AVAILABLE
				SeatAvailabilityStatus.SOLD_OUT, SeatAvailabilityStatus.AVAILABLE,
				false, true, false
			),
			new SeatStatusScenario(
				"6. 완전 매진 상황 - 모든 좌석 + 입석 매진",
				2, 1, 10, 8,   // 일반실 80석, 특실 24석 → 총 104석 → 입석 15석
				80, 20, 15,    // 일반실 매진, 특실 20석 예약 → 4석 잔여, 입석 매진
				6,             // 6명 요청 (특실 4석으로 수용 불가, 입석 매진)
				// 일반실: 0석 잔여, 입석 매진 → SOLD_OUT
				// 특실: 4 < 6명 요청 → INSUFFICIENT (입석 불가하므로)
				SeatAvailabilityStatus.SOLD_OUT, SeatAvailabilityStatus.INSUFFICIENT,
				false, false, false
			)
		);
	}

	@DisplayName("다양한 기존 예약 상황에 따라 적절한 좌석 상태와 입석 정보를 표시한다.")
	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("seatStatusScenarios")
	void shouldDisplayCorrectSeatAndStandingInfoForAllScenarios(SeatStatusScenario scenario) {
		// given
		LocalDate searchDate = LocalDate.now().plusDays(1);
		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 50000, 80000);
		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");
		Member member = memberRepository.save(MemberFixture.createStandardMember());

		Train train = trainTestHelper.createRealisticTrain(
			scenario.standardCars, scenario.firstClassCars,
			scenario.standardRows, scenario.firstClassRows);

		TrainScheduleWithStopStations schedule = trainScheduleTestHelper.createCustomSchedule()
			.scheduleName("KTX TEST")
			.operationDate(searchDate)
			.train(train)
			.addStop("서울", null, LocalTime.of(10, 0))
			.addStop("부산", LocalTime.of(13, 0), null)
			.build();

		ScheduleStop departureStop = trainScheduleTestHelper.getScheduleStopByStationName(schedule, "서울");
		ScheduleStop arrivalStop = trainScheduleTestHelper.getScheduleStopByStationName(schedule, "부산");

		if (scenario.reservedStandardSeats > 0) {
			List<Long> seatIds = trainTestHelper.getSeatIds(train, CarType.STANDARD, scenario.reservedStandardSeats);
			reservationTestHelper.createReservationWithSeatIds(member, schedule, departureStop, arrivalStop, seatIds,
				PassengerType.ADULT);
		}
		if (scenario.reservedFirstClassSeats > 0) {
			List<Long> seatIds = trainTestHelper.getSeatIds(train, CarType.FIRST_CLASS,
				scenario.reservedFirstClassSeats);
			reservationTestHelper.createReservationWithSeatIds(member, schedule, departureStop, arrivalStop, seatIds,
				PassengerType.ADULT);
		}
		if (scenario.reservedStandingSeats > 0) {
			reservationTestHelper.createStandingReservation(member, schedule, departureStop, arrivalStop,
				scenario.reservedStandingSeats);
		}

		// when
		TrainSearchRequest request = new TrainSearchRequest(seoul.getId(), busan.getId(), searchDate,
			scenario.passengerCount, "00");
		TrainSearchSlicePageResponse response = trainSearchService.searchTrains(request, PageRequest.of(0, 20));

		// then
		assertThat(response.content()).hasSize(1);
		TrainSearchResponse trainResult = response.content().get(0);

		assertThat(trainResult.standardSeat().status()).isEqualTo(scenario.expectedStandardStatus);
		assertThat(trainResult.standardSeat().canReserve()).isEqualTo(scenario.expectedStandardCanReserve);
		assertThat(trainResult.firstClassSeat().status()).isEqualTo(scenario.expectedFirstClassStatus);
		assertThat(trainResult.firstClassSeat().canReserve()).isEqualTo(scenario.expectedFirstClassCanReserve);
		assertThat(trainResult.hasStandingInfo()).isEqualTo(scenario.expectedHasStanding);

		if (scenario.expectedHasStanding) {
			assertThat(trainResult.standing()).isNotNull();
			assertThat(trainResult.standing().remainingStanding()).isGreaterThan(0);
			assertThat(trainResult.standing().fare()).isEqualTo((int)(50000 * 0.85));
			assertThat(trainResult.standardSeat().status()).isEqualTo(SeatAvailabilityStatus.STANDING_ONLY);
			assertThat(trainResult.standardSeat().displayText()).contains("입석");
		} else {
			assertThat(trainResult.standing()).isNull();
		}

		int expectedStandardTotal = scenario.standardCars * scenario.standardRows * 4;
		int expectedFirstClassTotal = scenario.firstClassCars * scenario.firstClassRows * 3;
		int expectedStandardRemaining = expectedStandardTotal - scenario.reservedStandardSeats;
		int expectedFirstClassRemaining = expectedFirstClassTotal - scenario.reservedFirstClassSeats;

		assertThat(trainResult.standardSeat().totalSeats()).isEqualTo(expectedStandardTotal);
		assertThat(trainResult.standardSeat().remainingSeats()).isEqualTo(expectedStandardRemaining);
		assertThat(trainResult.firstClassSeat().totalSeats()).isEqualTo(expectedFirstClassTotal);
		assertThat(trainResult.firstClassSeat().remainingSeats()).isEqualTo(expectedFirstClassRemaining);
	}

	/**
	 * 입석 테스트용 공통 데이터
	 */
	private void setupStandingTestData() {
		LocalDate searchDate = LocalDate.now().plusDays(1);
		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 50000, 80000);
		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");
		Member member = memberRepository.save(MemberFixture.createStandardMember());

		// 여유 열차와 매진 열차 생성
		Train availableTrain = trainTestHelper.createRealisticTrain(1, 1, 8, 6); // 일반실 32석, 특실 18석
		Train soldOutTrain = trainTestHelper.createRealisticTrain(1, 1, 8, 6);   // 일반실 32석, 특실 18석

		TrainScheduleWithStopStations availableSchedule = createTrainSchedule(availableTrain, searchDate,
			"KTX 201", LocalTime.of(10, 0), LocalTime.of(13, 0), "서울", "부산");
		TrainScheduleWithStopStations soldOutSchedule = createTrainSchedule(soldOutTrain, searchDate,
			"KTX 203", LocalTime.of(10, 10), LocalTime.of(13, 10), "서울", "부산");

		ScheduleStop departureStop = trainScheduleTestHelper.getScheduleStopByStationName(soldOutSchedule, "서울");
		ScheduleStop arrivalStop = trainScheduleTestHelper.getScheduleStopByStationName(soldOutSchedule, "부산");

		// 매진 열차의 일반실 모두 예약 (32석 모두)
		List<Long> allStandardSeats = trainTestHelper.getSeatIds(soldOutTrain, CarType.STANDARD, 32);
		reservationTestHelper.createReservationWithSeatIds(member, soldOutSchedule, departureStop, arrivalStop,
			allStandardSeats, PassengerType.ADULT);
	}

	@DisplayName("입석 정보 자동 제공: 일반실 여유 열차는 입석 정보를 제공하지 않고, 매진/부족 열차는 입석 정보를 제공한다.")
	@Test
	void shouldAutoProvideStandingInfoWhenStandardSoldOutOrInsufficient() {
		// given
		setupStandingTestData();
		LocalDate searchDate = LocalDate.now().plusDays(1);
		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		// when
		TrainSearchRequest request = new TrainSearchRequest(
			seoul.getId(), busan.getId(), searchDate, 2, "00");
		TrainSearchSlicePageResponse response = trainSearchService.searchTrains(request, PageRequest.of(0, 20));

		// then
		assertThat(response.content()).hasSize(2);

		TrainSearchResponse availableTrain = findTrainByTime(response.content(), LocalTime.of(10, 0));
		TrainSearchResponse soldOutTrain = findTrainByTime(response.content(), LocalTime.of(10, 10));

		assertThat(availableTrain.hasStandingInfo()).isFalse();
		assertThat(soldOutTrain.hasStandingInfo()).isTrue();

		log.info("여유 열차 - {}: 일반실 {}석 잔여, 입석 정보 없음",
			availableTrain.trainNumber(), availableTrain.standardSeat().remainingSeats());
		log.info("매진 열차 - {}: 일반실 매진, 입석 정보 제공",
			soldOutTrain.trainNumber());
	}

	@DisplayName("입석 요금 할인: 입석 요금은 일반실 대비 15% 할인을 적용한다. (85% 요금)")
	@Test
	void shouldApply15PercentDiscountOnStandingFare() {
		// given
		setupStandingTestData();
		LocalDate searchDate = LocalDate.now().plusDays(1);
		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		// when
		TrainSearchRequest request = new TrainSearchRequest(
			seoul.getId(), busan.getId(), searchDate, 3, "00");
		TrainSearchSlicePageResponse response = trainSearchService.searchTrains(request, PageRequest.of(0, 20));

		// then
		List<TrainSearchResponse> standingTrains = response.content().stream()
			.filter(TrainSearchResponse::hasStandingInfo)
			.toList();

		assertThat(standingTrains).hasSize(1);

		TrainSearchResponse standingTrain = standingTrains.get(0);
		int standingFare = standingTrain.standing().fare();
		int standardFare = standingTrain.standardSeat().fare();
		int expectedFare = (int)(standardFare * 0.85);
		double actualDiscount = (1.0 - (double)standingFare / standardFare) * 100;

		assertThat(standingFare).isEqualTo(expectedFare);
		assertThat(actualDiscount).isEqualTo(15.0, within(0.1)); // ±0.1 범위 오차 허용

		log.info("입석 요금 할인 검증 - 일반실: {}원, 입석: {}원 ({}% 할인)",
			standardFare, standingFare, String.format("%.1f", actualDiscount));
	}

	@DisplayName("입석 수용력: 열차는 총 좌석의 15%를 입석 인원으로 수용할 수 있다. (32+18=50석 → 7석)")
	@Test
	void shouldCalculateStandingCapacityAsFifteenPercent() {
		// given
		setupStandingTestData();
		LocalDate searchDate = LocalDate.now().plusDays(1);
		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		// when
		TrainSearchRequest request = new TrainSearchRequest(
			seoul.getId(), busan.getId(), searchDate, 4, "00");
		TrainSearchSlicePageResponse response = trainSearchService.searchTrains(request, PageRequest.of(0, 20));

		// then
		List<TrainSearchResponse> standingTrains = response.content().stream()
			.filter(TrainSearchResponse::hasStandingInfo)
			.toList();

		assertThat(standingTrains).hasSize(1);

		TrainSearchResponse standingTrain = standingTrains.get(0);
		int totalSeats = standingTrain.standardSeat().totalSeats() + standingTrain.firstClassSeat().totalSeats();
		int expectedCapacity = (int)(totalSeats * 0.15); // 50 * 0.15 = 7.5 → 7석

		assertThat(totalSeats).isEqualTo(50); // 32 + 18
		assertThat(expectedCapacity).isEqualTo(7);
		assertThat(standingTrain.standing().maxStanding()).isEqualTo(expectedCapacity);
		assertThat(standingTrain.standing().remainingStanding()).isLessThanOrEqualTo(expectedCapacity);

		log.info("입석 수용력 검증 - 총 좌석: {}석, 입석 용량: {}석, 잔여 입석: {}석",
			totalSeats, expectedCapacity, standingTrain.standing().remainingStanding());
	}

	@DisplayName("입석 상태 표시: 일반실 매진, 입석 예약 가능 시 좌석 상태는 STANDING_ONLY로 표시되고, 입석 정보가 노출된다.")
	@Test
	void shouldDisplayStandingOnlyStatusAndStandingInfoWhenStandardSoldOutAndCanReserveStanding() {
		// given
		setupStandingTestData();
		LocalDate searchDate = LocalDate.now().plusDays(1);
		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		// when
		TrainSearchRequest request = new TrainSearchRequest(
			seoul.getId(), busan.getId(), searchDate, 2, "00");
		TrainSearchSlicePageResponse response = trainSearchService.searchTrains(request, PageRequest.of(0, 20));

		// then
		TrainSearchResponse soldOutTrain = findTrainByTime(response.content(), LocalTime.of(10, 10));

		assertThat(soldOutTrain.hasStandingInfo()).isTrue();
		assertThat(soldOutTrain.standardSeat().status()).isEqualTo(SeatAvailabilityStatus.STANDING_ONLY);
		assertThat(soldOutTrain.standardSeat().canReserve()).isFalse();
		assertThat(soldOutTrain.standardSeat().displayText()).contains("일반실(입석)");
		assertThat(soldOutTrain.standing().remainingStanding()).isGreaterThan(0);

		log.info("입석 상태 표시 검증 - 일반실 상태: {}, 표시 텍스트: {}",
			soldOutTrain.standardSeat().status(), soldOutTrain.standardSeat().displayText());
	}

	/**
	 * 열차 스케줄 생성 헬퍼
	 */
	private TrainScheduleWithStopStations createTrainSchedule(Train train,
		LocalDate operationDate,
		String scheduleName, LocalTime departureTime, LocalTime arrivalTime,
		String departureStation, String arrivalStation) {
		return trainScheduleTestHelper.createCustomSchedule()
			.scheduleName(scheduleName)
			.operationDate(operationDate)
			.train(train)
			.addStop(departureStation, null, departureTime)
			.addStop(arrivalStation, arrivalTime, null)
			.build();
	}

	private TrainSearchResponse findTrainByTime(List<TrainSearchResponse> trains, LocalTime time) {
		return trains.stream()
			.filter(train -> train.departureTime().equals(time))
			.findFirst()
			.orElseThrow(() -> new AssertionError("시간 " + time + "에 해당하는 열차를 찾을 수 없습니다"));
	}
}
