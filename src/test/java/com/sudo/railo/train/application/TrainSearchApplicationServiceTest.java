package com.sudo.railo.train.application;

import static com.sudo.railo.support.helper.TrainScheduleTestHelper.*;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.application.dto.request.TrainCarListRequest;
import com.sudo.railo.train.application.dto.request.TrainCarSeatDetailRequest;
import com.sudo.railo.train.application.dto.request.TrainSearchRequest;
import com.sudo.railo.train.application.dto.response.OperationCalendarItem;
import com.sudo.railo.train.application.dto.response.TrainCarInfo;
import com.sudo.railo.train.application.dto.response.TrainCarListResponse;
import com.sudo.railo.train.application.dto.response.TrainCarSeatDetailResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchSlicePageResponse;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainCar;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.infrastructure.TrainCarRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceTest
class TrainSearchApplicationServiceTest {

	@Autowired
	private TrainSearchApplicationService trainSearchApplicationService;

	@Autowired
	private TrainTestHelper trainTestHelper;

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;

	@Autowired
	private TrainCarRepository trainCarRepository;

	@DisplayName("금일로부터 한달간의 운행 스케줄 캘린더를 조회한다.")
	@Test
	void getOperationCalendar() {
		// given
		Train train1 = trainTestHelper.createKTX();
		Train train2 = trainTestHelper.createCustomKTX(2, 1);

		LocalDate today = LocalDate.now();
		LocalDate tomorrow = today.plusDays(1);
		LocalDate dayAfterTomorrow = today.plusDays(2);
		LocalDate nextWeek = today.plusWeeks(1);

		createTrainSchedule(train1, today, "KTX 001", LocalTime.of(8, 0), LocalTime.of(11, 0));
		createTrainSchedule(train2, tomorrow, "KTX 003", LocalTime.of(14, 0), LocalTime.of(17, 0));
		createTrainSchedule(train1, nextWeek, "KTX 005", LocalTime.of(10, 0), LocalTime.of(13, 0));

		// when
		List<OperationCalendarItem> operationCalendar = trainSearchApplicationService.getOperationCalendar();

		// then
		// 1. 캘린더가 한 달치 날짜를 포함하는지 확인 (약 30일)
		assertThat(operationCalendar).hasSizeGreaterThanOrEqualTo(28).hasSizeLessThanOrEqualTo(32);

		// 2. 운행하는 날짜들이 isBookingAvailable = "Y"로 표시되는지 확인
		assertThat(operationCalendar).anyMatch(item ->
			item.operationDate().equals(today) && item.isBookingAvailable().equals("Y"));
		assertThat(operationCalendar).anyMatch(item ->
			item.operationDate().equals(tomorrow) && item.isBookingAvailable().equals("Y"));
		assertThat(operationCalendar).anyMatch(item ->
			item.operationDate().equals(nextWeek) && item.isBookingAvailable().equals("Y"));

		// 3. 운행하지 않는 날짜가 isBookingAvailable = "N"으로 표시되는지 확인
		assertThat(operationCalendar).anyMatch(item ->
			item.operationDate().equals(dayAfterTomorrow) && item.isBookingAvailable().equals("N"));

		// 4. 전체 캘린더에서 운행일과 비운행일이 모두 존재하는지 확인
		long operatingDays = operationCalendar.stream()
			.mapToLong(item -> item.isBookingAvailable().equals("Y") ? 1 : 0)
			.sum();
		long nonOperatingDays = operationCalendar.stream()
			.mapToLong(item -> item.isBookingAvailable().equals("N") ? 1 : 0)
			.sum();

		assertThat(operatingDays).isEqualTo(3); // today, tomorrow, nextWeek
		assertThat(nonOperatingDays).isGreaterThanOrEqualTo(0);
		assertThat(operatingDays + nonOperatingDays).isEqualTo(operationCalendar.size()); // 전체 합계 일치

		log.info("운행 캘린더 검증 완료: 전체 {} 일, 운행일 {} 일, 비운행일 {} 일",
			operationCalendar.size(), operatingDays, nonOperatingDays);
	}

	@DisplayName("검색 조건에 따른 열차를 조회한다.")
	@TestFactory
	List<DynamicTest> searchTrains() {
		// given
		Train train1 = trainTestHelper.createKTX();
		Train train2 = trainTestHelper.createCustomKTX(2, 1);
		Train train3 = trainTestHelper.createCustomKTX(3, 1);

		LocalDate futureDate = LocalDate.now().plusDays(1);

		// 오전, 오후, 저녁 시간대 열차 생성
		createTrainSchedule(train1, futureDate, "KTX 001", LocalTime.of(8, 0), LocalTime.of(11, 0));   // 오전
		createTrainSchedule(train2, futureDate, "KTX 003", LocalTime.of(14, 0), LocalTime.of(17, 0));  // 오후
		createTrainSchedule(train3, futureDate, "KTX 005", LocalTime.of(19, 0), LocalTime.of(22, 0));  // 저녁

		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 50000, 100000);

		// 검색 시나리오 정의
		record SearchScenario(
			String description,
			String departureHour,
			int expectedCount,
			java.util.function.Predicate<List<TrainSearchResponse>> validator
		) {
		}

		List<SearchScenario> scenarios = List.of(
			new SearchScenario(
				"전체 열차 조회 (06시 이후)",
				"06",
				3,
				trains -> trains.size() == 3 &&
					trains.stream().allMatch(train -> train.departureTime().isAfter(LocalTime.of(6, 0)))
			),
			new SearchScenario(
				"오후 이후 열차 조회 (13시 이후)",
				"13",
				2,
				trains -> trains.size() == 2 &&
					trains.stream().allMatch(train -> train.departureTime().isAfter(LocalTime.of(13, 0)))
			),
			new SearchScenario(
				"저녁 이후 열차 조회 (18시 이후)",
				"18",
				1,
				trains -> trains.size() == 1 &&
					trains.get(0).departureTime().isAfter(LocalTime.of(18, 0))
			),
			new SearchScenario(
				"심야 시간 조회 (23시 이후)",
				"23",
				0,
				trains -> trains.isEmpty()
			)
		);

		// DynamicTest 생성
		return scenarios.stream()
			.map(scenario -> DynamicTest.dynamicTest(
				scenario.description,
				() -> {
					// given
					TrainSearchRequest request = new TrainSearchRequest(
						seoul.getId(), busan.getId(), futureDate, 2, scenario.departureHour
					);
					Pageable pageable = PageRequest.of(0, 20);

					// when
					TrainSearchSlicePageResponse response = trainSearchApplicationService.searchTrains(request,
						pageable);

					// then
					assertThat(response.content()).hasSize(scenario.expectedCount);
					assertThat(scenario.validator.test(response.content())).isTrue();

					// 페이징 정보 기본 검증
					assertThat(response.currentPage()).isEqualTo(0);
					assertThat(response.first()).isTrue();
					assertThat(response.numberOfElements()).isEqualTo(scenario.expectedCount);

					log.info("검색 시나리오 완료 - {}: {}시 이후 → {}건 조회",
						scenario.description, scenario.departureHour, response.content().size());
				}
			))
			.toList();
	}

	@DisplayName("잔여 좌석이 있는 객차 목록을 조회할 수 있다.")
	@Test
	void getAvailableTrainCars() {
		// given
		Train train = trainTestHelper.createCustomKTX(3, 2);
		TrainScheduleWithStopStations scheduleWithStop = trainScheduleTestHelper.createSchedule(train);

		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		TrainCarListRequest request = new TrainCarListRequest(
			scheduleWithStop.trainSchedule().getId(),
			seoul.getId(),
			busan.getId(),
			2
		);

		// when
		TrainCarListResponse response = trainSearchApplicationService.getAvailableTrainCars(request);

		// then
		assertThat(response.trainClassificationCode()).isEqualTo("KTX");
		assertThat(response.trainNumber()).isNotBlank();
		assertThat(response.totalCarCount()).isGreaterThan(0);
		assertThat(response.totalCarCount()).isEqualTo(response.carInfos().size());

		List<String> carNumbers = response.carInfos().stream()
			.map(car -> car.carNumber())
			.toList();
		assertThat(carNumbers).contains(response.recommendedCarNumber());

		log.info("객차 목록 조회 완료: 열차 = {}-{}, 추천객차 = {}",
			response.trainClassificationCode(), response.trainNumber(), response.recommendedCarNumber());
	}

	// TODO : 객차 타입별 객차수 다채롭게 두어 테스트 필요
	@DisplayName("승객 수에 따라 추천에 적합한 객차(잔여 좌석수 > 승객 수)가 있다면 중간 객차를, 없으면 첫 번째 객차를 추천한다.")
	@TestFactory
	Collection<DynamicTest> getAvailableTrainCars_recommendationLogicScenarios() {
		// given
		Train train = trainTestHelper.createCustomKTX(6, 2);
		TrainScheduleWithStopStations scheduleWithStops = trainScheduleTestHelper.createSchedule(train);

		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		record RecommendationScenario(
			String description,
			int passengerCount,
			String expectedBehavior,
			Predicate<TrainCarListResponse> validator
		) {
		}

		List<RecommendationScenario> scenarios = List.of(
			new RecommendationScenario(
				"일반적인 승객 수 - 수용 가능한 객차 추천",
				2,
				"승객 수를 수용할 수 있는 객차 중에서 선택",
				response -> {
					// 추천 객차가 승객 수를 수용할 수 있는지
					TrainCarInfo recommendedCar = response.carInfos().stream()
						.filter(car -> car.carNumber().equals(response.recommendedCarNumber()))
						.findFirst()
						.orElse(null);

					return recommendedCar != null && recommendedCar.remainingSeats() >= 2;
				}
			),
			/*new RecommendationScenario(
				"많은 승객 수 - 최대 수용 가능한 객차 우선",
				9,
				"가장 많은 좌석을 가진 객차 선택 (일반실 우선)",
				response -> {
					TrainCarInfo recommendedCar = response.carInfos().stream()
						.filter(car -> car.carNumber().equals(response.recommendedCarNumber()))
						.findFirst()
						.orElse(null);

					if (recommendedCar == null)
						return false;

					// 추천 객차가 요청 승객 수를 수용할 수 있거나, 수용 불가능한 경우 가장 큰 객차여야 함
					boolean canAccommodate = recommendedCar.remainingSeats() >= 9;
					boolean isLargestCar = response.carInfos().stream()
						.allMatch(car -> car.remainingSeats() <= recommendedCar.remainingSeats());

					return canAccommodate || isLargestCar;
				}
			),*/
			new RecommendationScenario(
				"수용 불가능한 승객 수 - fallback 로직",
				20,
				"모든 객차가 수용 불가능할 때 첫 번째 객차 또는 가장 큰 객차 선택",
				response -> {
					TrainCarInfo recommendedCar = response.carInfos().stream()
						.filter(car -> car.carNumber().equals(response.recommendedCarNumber()))
						.findFirst()
						.orElse(null);

					if (recommendedCar == null)
						return false;

					// 모든 객차가 20명을 수용할 수 없으므로 fallback 로직 적용
					boolean isFirstCar = response.carInfos().get(0).carNumber().equals(response.recommendedCarNumber());
					boolean isLargestCar = response.carInfos().stream()
						.allMatch(car -> car.remainingSeats() <= recommendedCar.remainingSeats());

					return isFirstCar || isLargestCar;
				}
			)
		);

		return scenarios.stream()
			.map(scenario -> DynamicTest.dynamicTest(
				scenario.description + " (승객 " + scenario.passengerCount + "명)",
				() -> {
					// given
					TrainCarListRequest request = new TrainCarListRequest(
						scheduleWithStops.trainSchedule().getId(),
						seoul.getId(),
						busan.getId(),
						scenario.passengerCount
					);

					// when
					TrainCarListResponse response = trainSearchApplicationService.getAvailableTrainCars(request);

					// then
					assertThat(response.carInfos()).isNotEmpty();
					assertThat(response.recommendedCarNumber()).isNotBlank();
					assertThat(response.totalCarCount()).isEqualTo(2); // 일반실 1개 + 특실 1개

					// 추천 객차가 실제 객차 목록에 포함되어 있는지
					List<String> availableCarNumbers = response.carInfos().stream()
						.map(TrainCarInfo::carNumber)
						.toList();
					assertThat(availableCarNumbers).contains(response.recommendedCarNumber());

					// 각 객차 정보 상세 검증
					response.carInfos().forEach(carInfo -> {
						assertThat(carInfo.carNumber()).isNotBlank();
						assertThat(carInfo.carType()).isIn(CarType.STANDARD, CarType.FIRST_CLASS);
						assertThat(carInfo.totalSeats()).isGreaterThan(0);
						assertThat(carInfo.remainingSeats()).isGreaterThanOrEqualTo(0);
						assertThat(carInfo.remainingSeats()).isLessThanOrEqualTo(carInfo.totalSeats());
					});

					// 객차 타입별 분포 검증
					// TODO : 객차 타입별 객차수 다채롭게 두어 테스트 필요
					long standardCars = response.carInfos().stream()
						.mapToLong(car -> car.carType() == CarType.STANDARD ? 1 : 0)
						.sum();
					long firstClassCars = response.carInfos().stream()
						.mapToLong(car -> car.carType() == CarType.FIRST_CLASS ? 1 : 0)
						.sum();

					assertThat(standardCars + firstClassCars).isEqualTo(response.totalCarCount());
					assertThat(standardCars + firstClassCars).isEqualTo(response.carInfos().size());

					// 시나리오별 비즈니스 로직 검증
					assertThat(scenario.validator.test(response))
						.as("시나리오 '%s'의 추천 로직이 올바르게 작동해야 합니다. 추천객차: %s, 기대동작: %s",
							scenario.description, response.recommendedCarNumber(), scenario.expectedBehavior)
						.isTrue();

					TrainCarInfo recommendedCar = response.carInfos().stream()
						.filter(car -> car.carNumber().equals(response.recommendedCarNumber()))
						.findFirst()
						.orElseThrow();

					log.info("추천 로직 검증 완료 - {}: 승객{}명 → 객차{} (잔여{}석), 총 {}개 객차 중",
						scenario.description, scenario.passengerCount,
						response.recommendedCarNumber(), recommendedCar.remainingSeats(),
						response.carInfos().size());
				}
			))
			.toList();
	}

	@DisplayName("좌석 상세 조회")
	@Test
	void getTrainCarSeatDetail_delegatesToSeatQueryService() {
		// given
		Train train = trainTestHelper.createKTX();
		TrainScheduleWithStopStations scheduleWithStops = trainScheduleTestHelper.createSchedule(train);

		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		List<TrainCar> trainCars = trainCarRepository.findAllByTrainId(train.getId());
		TrainCar firstCar = trainCars.get(0);

		TrainCarSeatDetailRequest request = new TrainCarSeatDetailRequest(
			firstCar.getId(), scheduleWithStops.trainSchedule().getId(),
			seoul.getId(), busan.getId()
		);

		// when
		TrainCarSeatDetailResponse response = trainSearchApplicationService.getTrainCarSeatDetail(request);

		// then
		assertThat(response.carNumber()).isEqualTo(Integer.valueOf(firstCar.getCarNumber()).toString());
		assertThat(response.carType()).isEqualTo(firstCar.getCarType());
		assertThat(response.totalSeatCount()).isEqualTo(firstCar.getTotalSeats());
		assertThat(response.remainingSeatCount()).isGreaterThanOrEqualTo(0)
			.isLessThanOrEqualTo(firstCar.getTotalSeats());

		log.info("좌석 상세 조회 완료: 객차={}, 좌석타입={}",
			response.carNumber(), response.carType());
	}

	@DisplayName("전체 플로우 테스트 - 검색 -> 객차선택 -> 좌석조회 연동")
	@Test
	void fullSearchFlow_integrationTest() {
		// given
		Train train = trainTestHelper.createCustomKTX(2, 1);
		LocalDate futureDate = LocalDate.now().plusDays(1);
		createTrainSchedule(train, futureDate, "KTX 001", LocalTime.of(8, 0), LocalTime.of(11, 0));

		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 50000, 100000);

		// 1: 열차 검색
		TrainSearchRequest searchRequest = new TrainSearchRequest(
			seoul.getId(), busan.getId(), futureDate, 2, "00"
		);
		Pageable pageable = PageRequest.of(0, 20);

		TrainSearchSlicePageResponse searchResponse = trainSearchApplicationService.searchTrains(searchRequest,
			pageable);

		// 2: 검색된 열차의 객차 조회
		TrainCarListRequest carRequest = new TrainCarListRequest(
			searchResponse.content().get(0).trainScheduleId(),
			seoul.getId(), busan.getId(), 2
		);

		TrainCarListResponse carResponse = trainSearchApplicationService.getAvailableTrainCars(carRequest);

		// 3: 선택된 객차의 좌석 상세 조회
		String selectedCarNumber = carResponse.recommendedCarNumber();
		List<TrainCar> trainCars = trainCarRepository.findAllByTrainId(train.getId());
		TrainCar selectedCar = trainCars.stream()
			.filter(car -> String.format("%04d", car.getCarNumber()).equals(selectedCarNumber))
			.findFirst()
			.orElseThrow();

		TrainCarSeatDetailRequest seatRequest = new TrainCarSeatDetailRequest(
			selectedCar.getId(), searchResponse.content().get(0).trainScheduleId(),
			seoul.getId(), busan.getId()
		);

		TrainCarSeatDetailResponse seatResponse = trainSearchApplicationService.getTrainCarSeatDetail(seatRequest);

		// then
		// === Step 1: 열차 검색 결과 검증 ===
		assertThat(searchResponse.content()).hasSize(1);
		TrainSearchResponse searchResult = searchResponse.content().get(0);

		// 기본 열차 정보 검증
		assertThat(searchResult.trainScheduleId()).isNotNull();
		assertThat(searchResult.trainNumber()).isNotBlank();
		assertThat(searchResult.trainName()).isEqualTo("KTX");
		assertThat(searchResult.departureStationName()).isEqualTo("서울");
		assertThat(searchResult.arrivalStationName()).isEqualTo("부산");
		assertThat(searchResult.departureTime()).isEqualTo(LocalTime.of(8, 0));
		assertThat(searchResult.arrivalTime()).isEqualTo(LocalTime.of(11, 0));
		assertThat(searchResult.travelTime()).isEqualTo(Duration.ofHours(3));

		// 좌석 정보 검증
		assertThat(searchResult.standardSeat()).isNotNull();
		assertThat(searchResult.firstClassSeat()).isNotNull();
		assertThat(searchResult.standardSeat().fare()).isEqualTo(50000); // 일반실 요금
		assertThat(searchResult.firstClassSeat().fare()).isEqualTo(100000); // 특실 요금
		assertThat(searchResult.standardSeat().remainingSeats()).isGreaterThanOrEqualTo(0);
		assertThat(searchResult.firstClassSeat().remainingSeats()).isGreaterThanOrEqualTo(0);
		assertThat(searchResult.standardSeat().canReserve()).isNotNull();
		assertThat(searchResult.firstClassSeat().canReserve()).isNotNull();

		// === Step 2: 객차 조회 결과 검증 ===
		assertThat(carResponse.trainScheduleId()).isEqualTo(searchResult.trainScheduleId());
		assertThat(carResponse.trainClassificationCode()).isEqualTo("KTX");
		assertThat(carResponse.trainNumber()).isNotBlank();
		assertThat(carResponse.recommendedCarNumber()).isNotBlank();
		assertThat(carResponse.totalCarCount()).isEqualTo(2); // 일반실 1개 + 특실 1개
		assertThat(carResponse.carInfos()).hasSize(2);

		// 추천 객차가 실제 객차 목록에 존재하는지 검증
		List<String> availableCarNumbers = carResponse.carInfos().stream()
			.map(TrainCarInfo::carNumber)
			.toList();
		assertThat(availableCarNumbers).contains(carResponse.recommendedCarNumber());

		// 각 객차 정보 검증
		carResponse.carInfos().forEach(carInfo -> {
			assertThat(carInfo.carNumber()).isNotBlank();
			assertThat(carInfo.carType()).isIn(CarType.STANDARD, CarType.FIRST_CLASS);
			assertThat(carInfo.totalSeats()).isGreaterThan(0);
			assertThat(carInfo.remainingSeats()).isGreaterThanOrEqualTo(0);
			assertThat(carInfo.remainingSeats()).isLessThanOrEqualTo(carInfo.totalSeats());
		});

		// === Step 3: 좌석 상세 조회 결과 검증 ===
		assertThat(String.format("%04d", Integer.parseInt(seatResponse.carNumber()))).isEqualTo(selectedCarNumber);
		assertThat(seatResponse.carType()).isIn(CarType.STANDARD, CarType.FIRST_CLASS);
		assertThat(seatResponse.totalSeatCount()).isGreaterThan(0);
		assertThat(seatResponse.remainingSeatCount()).isGreaterThanOrEqualTo(0);
		assertThat(seatResponse.remainingSeatCount()).isLessThanOrEqualTo(seatResponse.totalSeatCount());
		assertThat(seatResponse.layoutType()).isIn(2, 3); // 2+2 또는 2+1 배치
		assertThat(seatResponse.seatList()).isNotEmpty();

		// 좌석 상세 정보 검증
		seatResponse.seatList().forEach(seat -> {
			assertThat(seat.seatId()).isNotNull();
			assertThat(seat.seatNumber()).isNotBlank();
			assertThat(seat.seatDirection()).isNotNull();
			assertThat(seat.seatType()).isNotNull();
			// available은 true/false 모두 가능
		});

		// 좌석 수 일관성 검증
		int totalSeatsInList = seatResponse.seatList().size();
		int availableSeatsInList = (int)seatResponse.seatList().stream()
			.mapToLong(seat -> seat.isAvailable() ? 1 : 0)
			.sum();

		assertThat(totalSeatsInList).isEqualTo(seatResponse.totalSeatCount());
		assertThat(availableSeatsInList).isEqualTo(seatResponse.remainingSeatCount());

		// === 플로우 간 데이터 일관성 검증 ===

		// 1. 검색 결과의 trainScheduleId가 모든 단계에서 일관되게 사용되는지
		assertThat(carRequest.trainScheduleId()).isEqualTo(searchResult.trainScheduleId());
		assertThat(seatRequest.trainScheduleId()).isEqualTo(searchResult.trainScheduleId());

		// 2. 선택된 객차가 실제 검색된 열차의 객차인지
		assertThat(selectedCar.getTrain().getId()).isEqualTo(train.getId());

		// 3. 객차 조회와 좌석 조회 간 데이터 일관성
		TrainCarInfo selectedCarInfo = carResponse.carInfos().stream()
			.filter(car -> car.carNumber().equals(selectedCarNumber) ||
				String.format("%04d", Integer.parseInt(car.carNumber())).equals(selectedCarNumber))
			.findFirst()
			.orElseThrow();

		assertThat(seatResponse.carType()).isEqualTo(selectedCarInfo.carType());
		assertThat(seatResponse.totalSeatCount()).isEqualTo(selectedCarInfo.totalSeats());
		assertThat(seatResponse.remainingSeatCount()).isEqualTo(selectedCarInfo.remainingSeats());

		// 4. 요청한 승객 수가 추천 객차에서 예약 가능한지 검증
		assertThat(selectedCarInfo.remainingSeats()).isGreaterThanOrEqualTo(2); // 요청한 승객 수

		// === 비즈니스 로직 검증 ===

		// 1. 추천 객차가 승객 수를 수용할 수 있는지
		assertThat(carResponse.carInfos().stream()
			.anyMatch(car -> car.carNumber().equals(carResponse.recommendedCarNumber()) &&
				car.remainingSeats() >= 2))
			.as("추천 객차는 요청한 승객 수(%d명)를 수용할 수 있어야 합니다", 2)
			.isTrue();

		// 2. 요금 정보가 올바르게 설정되었는지
		long standardSeatCars = carResponse.carInfos().stream()
			.mapToLong(car -> car.carType() == CarType.STANDARD ? 1 : 0)
			.sum();
		long firstClassCars = carResponse.carInfos().stream()
			.mapToLong(car -> car.carType() == CarType.FIRST_CLASS ? 1 : 0)
			.sum();

		assertThat(standardSeatCars).isEqualTo(1); // 일반실 1개
		assertThat(firstClassCars).isEqualTo(1);   // 특실 1개

		log.info("전체 플로우 테스트 완료: 검색{}건 → 객차{}개 → 좌석{}개, 추천객차={}, 잔여좌석={}",
			searchResponse.content().size(),
			carResponse.carInfos().size(),
			seatResponse.seatList().size(),
			carResponse.recommendedCarNumber(),
			seatResponse.remainingSeatCount());
	}

	private void createTrainSchedule(Train train, LocalDate operationDate, String scheduleName,
		LocalTime departureTime, LocalTime arrivalTime) {
		trainScheduleTestHelper.createCustomSchedule()
			.scheduleName(scheduleName)
			.operationDate(operationDate)
			.train(train)
			.addStop("서울", null, departureTime)
			.addStop("부산", arrivalTime, null)
			.build();
	}
}
