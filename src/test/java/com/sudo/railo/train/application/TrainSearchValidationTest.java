package com.sudo.railo.train.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.sudo.railo.booking.infrastructure.SeatReservationRepository;
import com.sudo.railo.booking.infrastructure.reservation.ReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.application.dto.request.TrainSearchRequest;
import com.sudo.railo.train.application.dto.response.TrainSearchSlicePageResponse;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.exception.TrainErrorCode;

import lombok.extern.slf4j.Slf4j;

@ServiceTest
@Slf4j
public class TrainSearchValidationTest {

	@Autowired
	private TrainSearchService trainSearchService;

	@Autowired
	private TrainTestHelper trainTestHelper;

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;

	@Autowired
	private SeatReservationRepository seatReservationRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@DisplayName("다양한 잘못된 검색 조건에 대해 적절한 비즈니스 예외가 발생한다")
	@TestFactory
	Collection<DynamicTest> shouldThrowAppropriateExceptionForInvalidSearchConditions() {
		// given
		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");
		LocalDate validDate = LocalDate.now().plusDays(1);

		int currentHour = LocalTime.now().getHour();
		int pastHour = (currentHour == 0 ? 0 : currentHour - 1);
		String pastHourString = String.format("%02d", pastHour);

		record ValidationScenario(
			String description,
			TrainSearchRequest request,
			Class<? extends Exception> expectedException,
			TrainErrorCode expectedErrorCode,
			String expectedMessageContains
		) {
			@Override
			public String toString() {
				return description;
			}
		}

		List<ValidationScenario> scenarios = List.of(
			new ValidationScenario(
				"출발역과 도착역이 동일한 경우",
				new TrainSearchRequest(seoul.getId(), seoul.getId(), validDate, 1, "00"),
				BusinessException.class,
				TrainErrorCode.INVALID_ROUTE,
				TrainErrorCode.INVALID_ROUTE.getMessage()
			),
			new ValidationScenario(
				"운행일이 너무 먼 미래인 경우 (3개월 후)",
				new TrainSearchRequest(seoul.getId(), busan.getId(), LocalDate.now().plusMonths(3), 1, "00"),
				BusinessException.class,
				TrainErrorCode.OPERATION_DATE_TOO_FAR,
				TrainErrorCode.OPERATION_DATE_TOO_FAR.getMessage()
			),
			new ValidationScenario(
				"과거 시각을 출발 시간으로 선택한 경우",
				new TrainSearchRequest(seoul.getId(), busan.getId(), LocalDate.now(), 1, pastHourString),
				BusinessException.class,
				TrainErrorCode.DEPARTURE_TIME_PASSED,
				TrainErrorCode.DEPARTURE_TIME_PASSED.getMessage()
			)
		);

		return scenarios.stream()
			.map(scenario -> DynamicTest.dynamicTest(
				scenario.description,
				() -> {
					// when & then
					assertThatThrownBy(() -> trainSearchService.searchTrains(
						scenario.request, PageRequest.of(0, 20)))
						.isInstanceOf(scenario.expectedException)
						.hasMessageContaining(scenario.expectedMessageContains);

					log.info("검증 실패 시나리오 완료 - {}: {} 발생",
						scenario.description, scenario.expectedException.getSimpleName());
				}
			))
			.toList();
	}

	@DisplayName("다양한 검색 시나리오에서 검색 결과가 없을 경우 빈 리스트를 반환한다.")
	@TestFactory
	Collection<DynamicTest> shouldReturnEmptyListForNonexistentRoutesAndDates() {
		// given - 기본 테스트 데이터
		Train train = trainTestHelper.createRealisticTrain(2, 1, 10, 6);
		LocalDate searchDate = LocalDate.now().plusDays(1);

		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 50000, 80000);
		createTrainSchedule(train, searchDate, "KTX 001",
			LocalTime.of(10, 0), LocalTime.of(13, 0), "서울", "부산");

		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");
		Station daegu = trainScheduleTestHelper.getOrCreateStation("대구");

		record NoResultScenario(
			String description,
			TrainSearchRequest request
		) {
			@Override
			public String toString() {
				return description;
			}
		}

		List<NoResultScenario> scenarios = List.of(
			new NoResultScenario(
				"존재하는 역이지만 해당 역을 경유하는 노선이 없는 경우 (서울-대구)",
				new TrainSearchRequest(seoul.getId(), daegu.getId(), searchDate, 1, "00")
			),
			new NoResultScenario(
				"해당 날짜에 운행하는 열차 없음",
				new TrainSearchRequest(seoul.getId(), busan.getId(), searchDate.plusDays(1), 1, "00")
			),
			new NoResultScenario(
				"요청한 출발 시간 이후에 운행하는 열차 없음",
				new TrainSearchRequest(seoul.getId(), busan.getId(), searchDate, 1, "15")
			)
		);

		return scenarios.stream()
			.map(scenario -> DynamicTest.dynamicTest(
				scenario.description,
				() -> {
					// when
					TrainSearchSlicePageResponse response = trainSearchService.searchTrains(
						scenario.request, PageRequest.of(0, 20));

					// then: 검색 결과 없을 때 빈 리스트 반환
					assertThat(response.content()).isEmpty();

					log.info("검색 결과 없음 시나리오 완료 - {}", scenario.description);
				}
			))
			.toList();
	}

	/**
	 * 열차 스케줄 생성 헬퍼
	 */
	private TrainScheduleTestHelper.TrainScheduleWithStopStations createTrainSchedule(Train train,
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
}
