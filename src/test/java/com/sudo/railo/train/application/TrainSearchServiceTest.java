package com.sudo.railo.train.application;

import static com.sudo.railo.support.helper.TrainScheduleTestHelper.*;
import static com.sudo.railo.train.exception.TrainErrorCode.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.helper.ReservationTestHelper;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.application.dto.request.TrainSearchRequest;
import com.sudo.railo.train.application.dto.response.TrainSearchResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchSlicePageResponse;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.exception.TrainErrorCode;

import lombok.extern.slf4j.Slf4j;

@ServiceTest
@Slf4j
class TrainSearchServiceTest {

	@Autowired
	private TrainSearchService trainSearchService;

	@Autowired
	private TrainTestHelper trainTestHelper;

	@Autowired
	private ReservationTestHelper reservationTestHelper;

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;

	@DisplayName("열차 조회 시 지정한 출발 시간 이후에만 필터링하고, 결과를 시간순으로 정렬해서 반환한다")
	@TestFactory
	Collection<DynamicTest> shouldFilterByDepartureHourAndSortChronologically() {
		// given
		Train train1 = trainTestHelper.createRealisticTrain(1, 1, 8, 4); // 일반실 32석, 특실 12석
		Train train2 = trainTestHelper.createRealisticTrain(1, 1, 8, 4);
		Train train3 = trainTestHelper.createRealisticTrain(1, 1, 8, 4);

		LocalDate searchDate = LocalDate.now().plusDays(1);

		// 요금 정보 생성
		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 50000, 80000);

		// 다양한 시간대의 열차 생성 (시간순 정렬 테스트를 위해)
		createTrainSchedule(train1, searchDate, "KTX 001", LocalTime.of(6, 0), LocalTime.of(9, 15), "서울", "부산");
		createTrainSchedule(train2, searchDate, "KTX 003", LocalTime.of(12, 30), LocalTime.of(15, 45), "서울", "부산");
		createTrainSchedule(train3, searchDate, "KTX 005", LocalTime.of(18, 0), LocalTime.of(21, 15), "서울", "부산");

		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		record TimeFilterScenario(
			String description,
			String departureHour,
			int expectedTrainCount,
			List<LocalTime> expectedDepartureTimes
		) {
		}

		List<TimeFilterScenario> scenarios = List.of(
			new TimeFilterScenario(
				"전체 열차 조회 (0시 이후)",
				"00",
				3,
				List.of(LocalTime.of(6, 0), LocalTime.of(12, 30), LocalTime.of(18, 0))
			),
			new TimeFilterScenario(
				"오전 중반 이후 열차 조회 (10시 이후)",
				"10",
				2,
				List.of(LocalTime.of(12, 30), LocalTime.of(18, 0))
			),
			new TimeFilterScenario(
				"오후 이후 열차 조회 (14시 이후)",
				"14",
				1,
				List.of(LocalTime.of(18, 0))
			),
			new TimeFilterScenario(
				"심야 시간 조회 (22시 이후)",
				"22",
				0,
				List.of()
			)
		);

		return scenarios.stream()
			.map(scenario -> DynamicTest.dynamicTest(
				scenario.description,
				() -> {
					// given
					TrainSearchRequest request = new TrainSearchRequest(
						seoul.getId(), busan.getId(), searchDate, 2, scenario.departureHour // 2명으로 고정 (수용력과 무관)
					);
					Pageable pageable = PageRequest.of(0, 20);

					// when
					TrainSearchSlicePageResponse response = trainSearchService.searchTrains(request, pageable);

					// then - 기본 검증
					assertThat(response.content()).hasSize(scenario.expectedTrainCount);
					assertThat(response.currentPage()).isEqualTo(0);
					assertThat(response.first()).isTrue();

					// 시간순 정렬 검증
					List<LocalTime> actualDepartureTimes = response.content().stream()
						.map(TrainSearchResponse::departureTime)
						.toList();

					assertThat(actualDepartureTimes)
						.as("출발 시간이 시간순으로 정렬되어야 합니다.")
						.isSorted()
						.containsExactlyElementsOf(scenario.expectedDepartureTimes);

					// 각 열차 기본 정보 검증
					response.content().forEach(train -> {
						assertThat(train.trainScheduleId()).isNotNull();
						assertThat(train.trainNumber()).isNotBlank();
						assertThat(train.trainName()).isEqualTo("KTX");
						assertThat(train.travelTime()).isPositive();
						assertThat(train.departureStationName()).isEqualTo("서울");
						assertThat(train.arrivalStationName()).isEqualTo("부산");
					});

					log.info("시간순 필터링 시나리오 완료 - {}: {}시 이후 → {}건 조회, 출발시간: {}",
						scenario.description, scenario.departureHour, response.content().size(),
						actualDepartureTimes.stream().map(Object::toString).collect(Collectors.joining(", ")));
				}
			))
			.toList();
	}

	@DisplayName("기본 일정 조회 시 존재하지 않는 스케줄 ID 로 조회하면 상세 오류 코드와 메시지가 포함된 예외를 던진다")
	@Test
	void getTrainScheduleBasicInfo_throwsInformativeExceptionForNonExistentScheduleId() {
		// given
		Long nonExistentScheduleId = 999999L;

		// when & then
		assertThatThrownBy(() -> trainSearchService.getTrainScheduleBasicInfo(nonExistentScheduleId))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(TRAIN_SCHEDULE_DETAIL_NOT_FOUND.getMessage());

		log.info("존재하지 않는 스케줄 ID({}) 조회 예외 처리 완료", nonExistentScheduleId);
	}

	@DisplayName("열차 조회 시 페이징이 올바르게 동작하고, 페이지 내*간 중복 없이 시간순 정렬을 유지한다")
	@Test
	void searchTrains_paginatesLargeResultsCorrectlyWithoutDuplicationAndMaintainsTimeOrdering() {
		// given
		LocalDate searchDate = LocalDate.now().plusDays(1);
		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 50000, 80000);

		// 다양한 크기와 시간대로 10개 열차 생성
		List<Train> trains = IntStream.range(0, 10)
			.mapToObj(i -> {
				int standardCars = 2 + (i % 3); // 2-4개 일반실
				int firstClassCars = 1 + (i % 2); // 1-2개 특실
				return trainTestHelper.createRealisticTrain(standardCars, firstClassCars, 10, 6);
			})
			.toList();

		for (int i = 0; i < trains.size(); i++) {
			createTrainSchedule(trains.get(i), searchDate,
				String.format("KTX %03d", i + 1),
				LocalTime.of(6 + i, 0), // 6시부터 1시간 간격
				LocalTime.of(9 + i, 0),
				"서울", "부산");
		}

		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		TrainSearchRequest request = new TrainSearchRequest(
			seoul.getId(), busan.getId(), searchDate, 1, "00"
		);

		// when
		record PagingTest(int pageSize, int expectedFirstPageSize, int expectedSecondPageSize) {
		}

		// 다양한 페이지 크기로 테스트
		List<PagingTest> pagingTests = List.of(
			new PagingTest(3, 3, 3), // 3개씩, 10개 → 3, 3, 3, 1
			new PagingTest(4, 4, 4), // 4개씩, 10개 → 4, 4, 2
			new PagingTest(7, 7, 3)  // 7개씩, 10개 → 7, 3
		);

		pagingTests.forEach(test -> {
			// 첫 번째 페이지
			Pageable firstPage = PageRequest.of(0, test.pageSize);
			TrainSearchSlicePageResponse firstResponse = trainSearchService.searchTrains(request, firstPage);

			// 두 번째 페이지
			Pageable secondPage = PageRequest.of(1, test.pageSize);
			TrainSearchSlicePageResponse secondResponse = trainSearchService.searchTrains(request, secondPage);

			// then - 첫 번째 페이지 검증
			assertThat(firstResponse.content()).hasSize(test.expectedFirstPageSize);
			assertThat(firstResponse.currentPage()).isEqualTo(0);
			assertThat(firstResponse.first()).isTrue();
			assertThat(firstResponse.hasNext()).isTrue();

			// then - 두 번째 페이지 검증
			assertThat(secondResponse.content()).hasSize(test.expectedSecondPageSize);
			assertThat(secondResponse.currentPage()).isEqualTo(1);
			assertThat(secondResponse.first()).isFalse();

			// 페이지 간 데이터 중복 없음 검증
			Set<Long> firstPageTrainScheduleIds = firstResponse.content().stream()
				.map(TrainSearchResponse::trainScheduleId)
				.collect(Collectors.toSet());
			Set<Long> secondPageTrainScheduleIds = secondResponse.content().stream()
				.map(TrainSearchResponse::trainScheduleId)
				.collect(Collectors.toSet());

			assertThat(firstPageTrainScheduleIds).doesNotContainAnyElementsOf(secondPageTrainScheduleIds);

			// 시간순 정렬 검증 (각 페이지 내에서)
			assertThat(firstResponse.content())
				.extracting(TrainSearchResponse::departureTime)
				.isSorted();
			assertThat(secondResponse.content())
				.extracting(TrainSearchResponse::departureTime)
				.isSorted();

			// 페이지 간 시간 순서 검증 (첫 페이지 마지막 < 둘째 페이지 첫번째)
			if (!firstResponse.content().isEmpty() && !secondResponse.content().isEmpty()) {
				LocalTime lastTimeFirstPage = firstResponse.content()
					.get(firstResponse.content().size() - 1)
					.departureTime();
				LocalTime firstTimeSecondPage = secondResponse.content().get(0).departureTime();
				assertThat(lastTimeFirstPage).isBefore(firstTimeSecondPage);
			}

			log.info("페이징 테스트 완료 (크기 {}): 1페이지 {}건, 2페이지 {}건",
				test.pageSize, firstResponse.content().size(), secondResponse.content().size());
		});
	}

	@DisplayName("조회하는 구간의 요금 정보가 없으면 STATION_FARE_NOT_FOUND 예외를 던진다")
	@Test
	void shouldThrowStationFareNotFoundWhenFareIsMissing() {
		// given
		LocalDate searchDate = LocalDate.now().plusDays(1);

		// 역 생성
		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		// 서울→부산 요금 등록
		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 50000, 80000);

		Train train = trainTestHelper.createRealisticTrain(1, 1, 10, 6);
		// 요금 없는 방향으로 부산→서울 스케줄 생성
		createTrainSchedule(train, searchDate, "KTX Rev",
			LocalTime.of(15, 0), LocalTime.of(18, 0),
			"부산", "서울"
		);

		// when & then
		TrainSearchRequest request = new TrainSearchRequest(
			busan.getId(),    // 출발: 요금 미등록 방향
			seoul.getId(),    // 도착
			searchDate,
			1,
			"15"              // 15시 이후
		);

		assertThatThrownBy(() ->
			trainSearchService.searchTrains(request, PageRequest.of(0, 10))
		)
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> {
				BusinessException be = (BusinessException)ex;
				assertThat(be.getErrorCode())
					.isEqualTo(TrainErrorCode.STATION_FARE_NOT_FOUND);
				assertThat(be.getMessage())
					.contains(TrainErrorCode.STATION_FARE_NOT_FOUND.getMessage());
			});
	}

	/**
	 * 열차 스케줄 생성 헬퍼
	 */
	private TrainScheduleWithStopStations createTrainSchedule(Train train, LocalDate operationDate,
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
}
