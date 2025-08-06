package com.sudo.railo.train.application;

import static com.sudo.railo.support.helper.TrainScheduleTestHelper.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sudo.railo.booking.infrastructure.SeatReservationRepository;
import com.sudo.railo.booking.infrastructure.reservation.ReservationRepository;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.helper.ReservationTestHelper;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.application.dto.request.TrainSearchRequest;
import com.sudo.railo.train.application.dto.response.TrainSearchResponse;
import com.sudo.railo.train.application.dto.response.TrainSearchSlicePageResponse;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.infrastructure.SeatRepository;
import com.sudo.railo.train.infrastructure.StationRepository;
import com.sudo.railo.train.infrastructure.TrainScheduleRepository;

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

	@Autowired
	private StationRepository stationRepository;

	@Autowired
	private TrainScheduleRepository trainScheduleRepository;

	@Autowired
	private SeatReservationRepository seatReservationRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SeatRepository seatRepository;

	@DisplayName("출발 시간 조건에 따라 해당 시간 이후 출발하는 열차들만 시간순으로 필터링되어 조회된다")
	@TestFactory
	Collection<DynamicTest> searchTrains_filtersTrainsByDepartureTimeAndSortsChronologically() {
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
