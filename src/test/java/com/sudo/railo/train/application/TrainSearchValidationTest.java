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
import com.sudo.railo.train.domain.Station;
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

	@DisplayName("다양한 잘못된 검색 조건에 대해 적절한 예외가 발생한다")
	@TestFactory
	Collection<DynamicTest> searchTrains_throwsAppropriateExceptionsForVariousInvalidConditions() {
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
}
