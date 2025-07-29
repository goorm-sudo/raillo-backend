package com.sudo.railo.booking.application;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sudo.railo.booking.application.dto.request.ReservationCreateRequest;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.status.ReservationStatus;
import com.sudo.railo.booking.domain.type.PassengerSummary;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.booking.domain.type.TripType;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.fixture.MemberFixture;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.support.helper.TrainScheduleTestHelper.TrainScheduleWithStopStations;
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.type.CarType;

import lombok.extern.slf4j.Slf4j;

@ServiceTest
@Slf4j
class ReservationServiceTest {

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private TrainTestHelper trainTestHelper;

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;


	@Test
	@DisplayName("유효한 요청으로 예약이 성공한다")
	void createReservation_success() {
		//given
		Train train = trainTestHelper.saveKTX();
		TrainScheduleWithStopStations scheduleWithStops = trainScheduleTestHelper.createSeoulToBusanSchedule(train);
		ScheduleStop departureStop = trainScheduleTestHelper.getScheduleStopByStationName(scheduleWithStops, "서울");
		ScheduleStop arrivalStop = trainScheduleTestHelper.getScheduleStopByStationName(scheduleWithStops, "부산");
		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 60700, 85000);

		// 일반실 좌석 2개 선택
		List<Long> standardSeatIds = trainTestHelper.getSeatIds(train, CarType.STANDARD, 2);

		Member member = MemberFixture.MEMBER();

		PassengerSummary adult = new PassengerSummary(PassengerType.ADULT, 1);
		PassengerSummary child = new PassengerSummary(PassengerType.CHILD, 1);
		ReservationCreateRequest request = new ReservationCreateRequest(
			scheduleWithStops.trainSchedule().getId(),
			departureStop.getStation().getId(),
			arrivalStop.getStation().getId(),
			List.of(adult, child),
			standardSeatIds,
			TripType.OW
		);

		// when
		memberRepository.save(member);
		Reservation reservation = reservationService.createReservation(request, member.getMemberDetail().getMemberNo());

		// then
		assertThat(reservation.getTrainSchedule().getId()).isEqualTo(scheduleWithStops.trainSchedule().getId());
		assertThat(reservation.getMember().getId()).isEqualTo(member.getId());
		assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(reservation.getTotalPassengers()).isEqualTo(2);
		assertThat(reservation.getReservationCode()).isNotNull();
	}
}
