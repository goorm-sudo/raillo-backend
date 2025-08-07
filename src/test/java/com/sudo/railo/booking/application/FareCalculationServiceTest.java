package com.sudo.railo.booking.application;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.sudo.railo.booking.domain.type.PassengerSummary;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.type.CarType;

@ServiceTest
class FareCalculationServiceTest {

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;

	@Autowired
	private FareCalculationService fareCalculationService;

	@DisplayName("운임 계산에 성공한다")
	@ParameterizedTest(name = "{index}. {1} 계산 결과 = {2}")
	@MethodSource("providePassengers")
	void calculateFare_success(
		List<PassengerSummary> passengers,
		CarType carType,
		BigDecimal expectedFare
	) {
		// given
		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");
		trainScheduleTestHelper.createOrUpdateStationFare(seoul.getStationName(), busan.getStationName(),
			50000, 100000);

		// when
		BigDecimal totalFare = fareCalculationService.calculateFare(seoul.getId(), busan.getId(), passengers, carType);

		// then
		assertThat(totalFare).isEqualByComparingTo(expectedFare);
	}

	private static Stream<Arguments> providePassengers() {
		return Stream.of(
			// 어른 2명 + 어린이 1명
			Arguments.of(
				List.of(
					new PassengerSummary(PassengerType.ADULT, 2),
					new PassengerSummary(PassengerType.CHILD, 1)
				),
				CarType.STANDARD,
				BigDecimal.valueOf(130000)
			),
			// 어른 1명
			Arguments.of(
				List.of(
					new PassengerSummary(PassengerType.ADULT, 1),
					new PassengerSummary(PassengerType.CHILD, 0)
				),
				CarType.FIRST_CLASS,
				BigDecimal.valueOf(100000)
			)
		);
	}
}
