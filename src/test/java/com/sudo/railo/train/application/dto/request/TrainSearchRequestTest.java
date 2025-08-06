package com.sudo.railo.train.application.dto.request;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.sudo.railo.support.annotation.ServiceTest;

import jakarta.validation.Validator;

@ServiceTest
class TrainSearchRequestTest {

	@Autowired
	private Validator validator;

	record ValidationScenario(
		String description,
		TrainSearchRequest request,
		String field,
		String expectedMessage
	) {
		@Override
		public String toString() {
			return description;
		}
	}

	static Stream<ValidationScenario> requestValidationScenarios() {
		LocalDate today = LocalDate.now();
		return Stream.of(
			new ValidationScenario(
				"출발역이 null인 경우",
				new TrainSearchRequest(null, 1L, today, 1, "00"),
				"departureStationId",
				"출발역을 선택해주세요"
			),
			new ValidationScenario(
				"도착역이 null인 경우",
				new TrainSearchRequest(1L, null, today, 1, "00"),
				"arrivalStationId",
				"도착역을 선택해주세요"
			),
			new ValidationScenario(
				"운행날짜가 과거인 경우",
				new TrainSearchRequest(1L, 2L, today.minusDays(1), 1, "00"),
				"operationDate",
				"운행날짜는 오늘 이후여야 합니다"
			),
			new ValidationScenario(
				"승객 수가 0명인 경우",
				new TrainSearchRequest(1L, 2L, today, 0, "00"),
				"passengerCount",
				"승객 수는 최소 1명이어야 합니다"
			),
			new ValidationScenario(
				"승객 수가 10명인 경우",
				new TrainSearchRequest(1L, 2L, today, 10, "00"),
				"passengerCount",
				"승객 수는 최대 9명까지 가능합니다"
			),
			new ValidationScenario(
				"출발 희망 시간이 blank인 경우",
				new TrainSearchRequest(1L, 2L, today, 1, ""),
				"departureHour",
				"출발 희망 시간을 선택해주세요"
			),
			new ValidationScenario(
				"잘못된 departureHour 형식(25시)",
				new TrainSearchRequest(1L, 2L, today, 1, "25"),
				"departureHour",
				"출발 시간은 00~23 사이의 정시 값이어야 합니다"
			)
		);
	}

	@DisplayName("잘못된 TrainSearchRequest DTO는 검증 예외가 발생한다.")
	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("requestValidationScenarios")
	void shouldFailValidationForInvalidDto(ValidationScenario scenario) {
		var violations = validator.validate(scenario.request());
		assertThat(violations)
			.anySatisfy(v -> {
				assertThat(v.getPropertyPath().toString()).isEqualTo(scenario.field());
				assertThat(v.getMessage()).isEqualTo(scenario.expectedMessage());
			});
	}
}
