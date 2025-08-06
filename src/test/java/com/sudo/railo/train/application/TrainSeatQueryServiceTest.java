package com.sudo.railo.train.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sudo.railo.booking.application.ReservationApplicationService;
import com.sudo.railo.booking.application.dto.request.ReservationCreateRequest;
import com.sudo.railo.booking.domain.type.PassengerSummary;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.booking.domain.type.TripType;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.fixture.MemberFixture;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.support.helper.TrainScheduleTestHelper.TrainScheduleWithStopStations;
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.application.dto.request.TrainCarSeatDetailRequest;
import com.sudo.railo.train.application.dto.response.SeatDetail;
import com.sudo.railo.train.application.dto.response.TrainCarInfo;
import com.sudo.railo.train.application.dto.response.TrainCarSeatDetailResponse;
import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.TrainCar;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.exception.TrainErrorCode;
import com.sudo.railo.train.infrastructure.TrainCarRepository;

@ServiceTest
class TrainSeatQueryServiceTest {

	@Autowired
	private TrainSeatQueryService trainSeatQueryService;

	@Autowired
	private TrainTestHelper trainTestHelper;

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;

	@Autowired
	private TrainCarRepository trainCarRepository;

	@Autowired
	private ReservationApplicationService reservationApplicationService;

	@Autowired
	private MemberRepository memberRepository;

	private TrainScheduleWithStopStations scheduleWithStops;
	private ScheduleStop departureStop;
	private ScheduleStop arrivalStop;
	private Train train;

	@BeforeEach
	void setUp() {
		train = trainTestHelper.createCustomKTX(1, 1);
		scheduleWithStops = trainScheduleTestHelper.createCustomSchedule()
			.scheduleName("test-schedule")
			.operationDate(LocalDate.now())
			.train(train)
			.addStop("서울", null, LocalTime.of(9, 30))
			.addStop("대전", LocalTime.of(10, 30), LocalTime.of(10, 32))
			.addStop("부산", LocalTime.of(12, 30), null)
			.build();

		departureStop = trainScheduleTestHelper.getScheduleStopByStationName(scheduleWithStops, "서울");
		arrivalStop = trainScheduleTestHelper.getScheduleStopByStationName(scheduleWithStops, "부산");
		trainScheduleTestHelper.createOrUpdateStationFare("서울", "부산", 50000, 10000);
	}

	@Test
	@DisplayName("잔여 좌석이 있는 객차 목록을 성공적으로 조회한다")
	void getAvailableTrainCars() {
		List<TrainCarInfo> availableTrainCars = trainSeatQueryService.getAvailableTrainCars(
			scheduleWithStops.trainSchedule().getId(),
			departureStop.getStation().getId(),
			arrivalStop.getStation().getId()
		);

		assertThat(availableTrainCars).hasSize(2);

		TrainCarInfo standardCar = availableTrainCars.get(0);
		assertThat(standardCar.id()).isEqualTo(1L);
		assertThat(standardCar.carNumber()).isEqualTo("0001");
		assertThat(standardCar.carType()).isEqualTo(CarType.STANDARD);
		assertThat(standardCar.totalSeats()).isEqualTo(2);
		assertThat(standardCar.remainingSeats()).isEqualTo(2);

		TrainCarInfo firstClassCar = availableTrainCars.get(1);
		assertThat(firstClassCar.id()).isEqualTo(2L);
		assertThat(firstClassCar.carNumber()).isEqualTo("0002");
		assertThat(firstClassCar.carType()).isEqualTo(CarType.FIRST_CLASS);
		assertThat(firstClassCar.totalSeats()).isEqualTo(2);
		assertThat(firstClassCar.remainingSeats()).isEqualTo(2);
	}

	@Test
	@DisplayName("예약된 좌석이 있으면 해당 좌석은 조회 되지 않는다")
	void shouldExcludeReservedSeatsFromAvailableCount() {
		Member testMember = MemberFixture.createStandardMember();
		memberRepository.save(testMember);

		List<Long> standardSeatIds = trainTestHelper.getSeatIds(train, CarType.STANDARD, 1);
		ReservationCreateRequest standardRequest = getReservationCreateRequest(standardSeatIds);
		reservationApplicationService.createReservation(standardRequest, testMember.getMemberDetail().getMemberNo());

		List<TrainCarInfo> availableTrainCars = trainSeatQueryService.getAvailableTrainCars(
			scheduleWithStops.trainSchedule().getId(),
			departureStop.getStation().getId(),
			arrivalStop.getStation().getId()
		);

		TrainCarInfo standardCar = availableTrainCars.get(0);
		assertThat(standardCar.carType()).isEqualTo(CarType.STANDARD);
		assertThat(standardCar.totalSeats()).isEqualTo(2);
		assertThat(standardCar.remainingSeats()).isEqualTo(1);
	}

	@Test
	@DisplayName("잔여 좌석이 없으면 조회 되지 않는다")
	void shouldThrowExceptionWhenNoAvailableSeats() {
		Member testMember = MemberFixture.createStandardMember();
		memberRepository.save(testMember);

		List<Long> standardSeatIds = trainTestHelper.getSeatIds(train, CarType.STANDARD, 2);
		ReservationCreateRequest standardRequest = getReservationCreateRequest(standardSeatIds);
		reservationApplicationService.createReservation(standardRequest, testMember.getMemberDetail().getMemberNo());

		List<Long> firstClassSeatIds = trainTestHelper.getSeatIds(train, CarType.FIRST_CLASS, 2);
		ReservationCreateRequest firstClassRequest = getReservationCreateRequest(firstClassSeatIds);
		reservationApplicationService.createReservation(firstClassRequest, testMember.getMemberDetail().getMemberNo());

		assertThatThrownBy(() -> trainSeatQueryService.getAvailableTrainCars(
			scheduleWithStops.trainSchedule().getId(), departureStop.getStation().getId(),
			arrivalStop.getStation().getId()
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TrainErrorCode.NO_AVAILABLE_CARS.getMessage());
	}

	@Test
	@DisplayName("존재하지 않는 열차 스케줄로 객차 조회 시 예외가 발생한다")
	void shouldThrowExceptionWhenGetAvailableTrainCarsWithTrainScheduleNotFound() {
		assertThatThrownBy(() -> trainSeatQueryService.getAvailableTrainCars(
			999L,
			departureStop.getStation().getId(),
			arrivalStop.getStation().getId()
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TrainErrorCode.TRAIN_SCHEDULE_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("존재하지 않는 출발역으로 객차 조회 시 예외가 발생한다")
	void shouldThrowExceptionWhenGetAvailableTrainCarsWithDepartureStationNotFound() {
		assertThatThrownBy(() -> trainSeatQueryService.getAvailableTrainCars(
			scheduleWithStops.trainSchedule().getId(),
			999L,
			arrivalStop.getStation().getId()
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TrainErrorCode.STATION_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("존재하지 않는 도착역으로 객차 조회 시 예외가 발생한다")
	void shouldThrowExceptionWhenGetAvailableTrainCarsWithArrivalStationNotFound() {
		assertThatThrownBy(() -> trainSeatQueryService.getAvailableTrainCars(
			scheduleWithStops.trainSchedule().getId(),
			departureStop.getStation().getId(),
		999L
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TrainErrorCode.STATION_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("유효하지 않은 경로로 조회하면 예외가 발생한다")
	void shouldThrowExceptionWhenGetAvailableTrainCarsWithInvalidRoute() {
		assertThatThrownBy(() -> trainSeatQueryService.getAvailableTrainCars(
			scheduleWithStops.trainSchedule().getId(),
			arrivalStop.getStation().getId(),
			arrivalStop.getStation().getId()
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TrainErrorCode.INVALID_ROUTE.getMessage());
	}

	@Test
	@DisplayName("객차 좌석 상세 정보를 성공적으로 조회한다")
	void getTrainCarSeatDetail() {
		List<TrainCar> trainCars = trainCarRepository.findByTrainIn(List.of(train));
		TrainCar trainCar = trainCars.get(0);
		TrainCarSeatDetailRequest request = new TrainCarSeatDetailRequest(
			trainCar.getId(),
			scheduleWithStops.trainSchedule().getId(),
			departureStop.getStation().getId(),
			arrivalStop.getStation().getId()
		);

		TrainCarSeatDetailResponse response = trainSeatQueryService.getTrainCarSeatDetail(request);

		assertThat(response.carNumber()).isEqualTo("1");
		assertThat(response.carType()).isEqualTo(CarType.STANDARD);
		assertThat(response.totalSeatCount()).isEqualTo(2);
		assertThat(response.remainingSeatCount()).isEqualTo(2);
		assertThat(response.layoutType()).isEqualTo(2);
		assertThat(response.seatList()).hasSize(2);

		SeatDetail firstSeatDetail = response.seatList().get(0);
		assertThat(firstSeatDetail.seatNumber()).isEqualTo("1A");
		assertThat(firstSeatDetail.isAvailable()).isTrue();

		SeatDetail secondSeatDetail = response.seatList().get(1);
		assertThat(secondSeatDetail.seatNumber()).isEqualTo("1B");
		assertThat(secondSeatDetail.isAvailable()).isTrue();
	}

	@Test
	@DisplayName("예약된 좌석은 조회시 사용 불가능한 상태로 조회된다.")
	void shouldReservedSeatsAsUnavailable() {
		Member testMember = MemberFixture.createStandardMember();
		memberRepository.save(testMember);
		List<Long> standardSeatIds = trainTestHelper.getSeatIds(train, CarType.STANDARD, 1);
		ReservationCreateRequest standardRequest = getReservationCreateRequest(standardSeatIds);
		reservationApplicationService.createReservation(standardRequest, testMember.getMemberDetail().getMemberNo());
		List<TrainCar> trainCars = trainCarRepository.findByTrainIn(List.of(train));
		TrainCar trainCar = trainCars.get(0);
		TrainCarSeatDetailRequest request = new TrainCarSeatDetailRequest(
			trainCar.getId(),
			scheduleWithStops.trainSchedule().getId(),
			departureStop.getStation().getId(),
			arrivalStop.getStation().getId()
		);

		TrainCarSeatDetailResponse response = trainSeatQueryService.getTrainCarSeatDetail(request);

		SeatDetail secondSeatDetail = response.seatList().get(0);
		assertThat(secondSeatDetail.isAvailable()).isFalse();
	}

	@Test
	@DisplayName("존재하지 않는 객차로 좌석 상세 조회 시 예외가 발생한다")
	void shouldThrowExceptionWhenGetTrainCarSeatDetailWithTrainCarNotFound() {
		TrainCarSeatDetailRequest request = new TrainCarSeatDetailRequest(
			999L,
			scheduleWithStops.trainSchedule().getId(),
			departureStop.getStation().getId(),
			arrivalStop.getStation().getId()
		);

		assertThatThrownBy(() -> trainSeatQueryService.getTrainCarSeatDetail(request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TrainErrorCode.TRAIN_CAR_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("존재하지 않는 열차 스케줄로 좌석 상세 조회 시 예외가 발생한다")
	void shouldThrowExceptionWhenGetTrainCarSeatDetailWithTrainScheduleNotFound() {
		List<TrainCar> trainCars = trainCarRepository.findByTrainIn(List.of(train));
		TrainCar trainCar = trainCars.get(0);
		TrainCarSeatDetailRequest request = new TrainCarSeatDetailRequest(
			trainCar.getId(),
			999L,
			departureStop.getStation().getId(),
			arrivalStop.getStation().getId()
		);

		assertThatThrownBy(() -> trainSeatQueryService.getTrainCarSeatDetail(request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TrainErrorCode.TRAIN_SCHEDULE_NOT_FOUND.getMessage());
	}

	private ReservationCreateRequest getReservationCreateRequest(List<Long> seatIds) {
		List<PassengerSummary> passengers = List.of(new PassengerSummary(PassengerType.ADULT, seatIds.size()));

		return new ReservationCreateRequest(
			scheduleWithStops.trainSchedule().getId(),
			departureStop.getStation().getId(),
			arrivalStop.getStation().getId(),
			passengers,
			seatIds,
			TripType.OW
		);
	}
}
