package com.sudo.railo.train.application;

import static com.sudo.railo.support.helper.TrainScheduleTestHelper.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

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

import lombok.extern.slf4j.Slf4j;

@ServiceTest
@Slf4j
@DisplayName("다구간 경로에서 예약 겹침(overlap) 로직 검증")
public class TrainSearchServiceOverlapReservationTest {

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

	record OverlapScenario(
		String description,
		String existingReservationRoute,              // ex. "서울-대전"
		String searchRoute,                          // ex. "대전-부산"
		List<Integer> reservedSeatsPerSegment,        // ex. 15
		int expectedRemainingSeats                  // ex. 120
	) {
		@Override
		public String toString() {
			return description;
		}
	}

	static Stream<OverlapScenario> overlapScenarios() {
		return Stream.of(
			new OverlapScenario(
				"기존 예약 : 서울→대전(15) / 검색 : 대전→부산 (비겹침)",
				"서울-대전", "대전-부산",
				List.of(15), 120
			),
			new OverlapScenario(
				"기존 예약 : 서울→부산(20) / 검색 : 대전→대구 (완전 겹침)",
				"서울-부산", "대전-대구",
				List.of(20), 100
			),
			new OverlapScenario(
				"기존 예약 : 대전→부산(25) / 검색 : 서울→대구 (부분 겹침)",
				"대전-부산", "서울-대구",
				List.of(25), 95
			),
			new OverlapScenario(
				"기존 예약 : 서울→대전(10) + 대구→부산(20) / 검색 : 대전→대구 (교집합 없음=비겹침)",
				"서울-대전+대구-부산", "대전-대구",
				List.of(15, 20), 120
			)
		);
	}

	@DisplayName("열차 조회 시 다구간 경로에서 기존의 ‘겹침 구간’ 예약만 잔여 좌석에서 차감된다.")
	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("overlapScenarios")
	void shouldCountOnlyOverlappingReservations(OverlapScenario s) {
		// given
		Train train = trainTestHelper.createRealisticTrain(3, 2, 10, 6); // 일반실 : 120석, 특실 : 36석
		LocalDate searchDate = LocalDate.now().plusDays(1);

		// 요금 설정
		trainScheduleTestHelper.createOrUpdateStationFare("서울", "대전", 25000, 40000);
		trainScheduleTestHelper.createOrUpdateStationFare("대전", "대구", 20000, 32000);
		trainScheduleTestHelper.createOrUpdateStationFare("대구", "부산", 15000, 24000);
		trainScheduleTestHelper.createOrUpdateStationFare("서울", "대구", 40000, 64000);
		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 50000, 80000);
		trainScheduleTestHelper.createOrUpdateStationFare("대전", "부산", 30000, 48000);

		TrainScheduleWithStopStations schedule = trainScheduleTestHelper.createCustomSchedule()
			.scheduleName("KTX 복합구간")
			.operationDate(searchDate)
			.train(train)
			.addStop("서울", null, LocalTime.of(8, 0))
			.addStop("대전", LocalTime.of(9, 0), LocalTime.of(9, 5))
			.addStop("대구", LocalTime.of(10, 30), LocalTime.of(10, 35))
			.addStop("부산", LocalTime.of(12, 0), null)
			.build();

		Station seoul = trainScheduleTestHelper.getOrCreateStation("서울");
		Station daejeon = trainScheduleTestHelper.getOrCreateStation("대전");
		Station daegu = trainScheduleTestHelper.getOrCreateStation("대구");
		Station busan = trainScheduleTestHelper.getOrCreateStation("부산");

		ScheduleStop seoulStop = trainScheduleTestHelper.getScheduleStopByStationName(schedule, "서울");
		ScheduleStop daejeonStop = trainScheduleTestHelper.getScheduleStopByStationName(schedule, "대전");
		ScheduleStop daeguStop = trainScheduleTestHelper.getScheduleStopByStationName(schedule, "대구");
		ScheduleStop busanStop = trainScheduleTestHelper.getScheduleStopByStationName(schedule, "부산");

		Member member = memberRepository.save(MemberFixture.createStandardMember());

		// 예약 생성
		String[] segments = s.existingReservationRoute().split("\\+");
		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			String[] stops = segment.split("-");
			ScheduleStop departureStop = trainScheduleTestHelper.getScheduleStopByStationName(schedule, stops[0]);
			ScheduleStop arrivalStop = trainScheduleTestHelper.getScheduleStopByStationName(schedule, stops[1]);

			int seatsToReserve = s.reservedSeatsPerSegment().get(i);
			List<Long> seatIds = trainTestHelper.getSeatIds(train, CarType.STANDARD, seatsToReserve);

			reservationTestHelper.createReservationWithSeatIds(
				member, schedule, departureStop, arrivalStop, seatIds, PassengerType.ADULT
			);
		}

		// when
		String[] searchNodes = s.searchRoute().split("-");
		Station searchDepartureStation = switch (searchNodes[0]) {
			case "서울" -> seoul;
			case "대전" -> daejeon;
			case "대구" -> daegu;
			default -> busan;
		};
		Station searchArrivalStation = switch (searchNodes[1]) {
			case "대전" -> daejeon;
			case "대구" -> daegu;
			case "부산" -> busan;
			default -> seoul;
		};

		TrainSearchRequest request = new TrainSearchRequest(
			searchDepartureStation.getId(),
			searchArrivalStation.getId(),
			searchDate,
			10,
			"00"
		);
		TrainSearchSlicePageResponse response = trainSearchService.searchTrains(request, PageRequest.of(0, 20));

		// then
		assertThat(response.content()).hasSize(1);
		TrainSearchResponse result = response.content().get(0);
		assertThat(result.standardSeat().remainingSeats()).isEqualTo(s.expectedRemainingSeats());
	}
}
