package com.sudo.railo.booking.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.type.CarType;

@ServiceTest
public class ReservationConcurrentConflictTest {

	private final int threadCount = 10;

	@Autowired
	private TrainTestHelper trainTestHelper;

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ReservationApplicationService reservationApplicationService;

	private TrainScheduleTestHelper.TrainScheduleWithStopStations scheduleWithStops;
	private String memberNo;
	private List<PassengerSummary> passengers;
	private List<Long> standardSeatIds;

	@BeforeEach
	void setUp() {
		Train train = trainTestHelper.createKTX();
		scheduleWithStops = trainScheduleTestHelper.createCustomSchedule()
			.scheduleName("test-schedule")
			.operationDate(LocalDate.now())
			.train(train)
			.addStop("1", null, LocalTime.of(9, 30))
			.addStop("2", LocalTime.of(10, 30), LocalTime.of(10, 30))
			.addStop("3", LocalTime.of(11, 0), LocalTime.of(11, 0))
			.addStop("4", LocalTime.of(11, 30), null)
			.build();

		Member member = MemberFixture.createStandardMember();
		memberRepository.save(member);
		memberNo = member.getMemberDetail().getMemberNo();
		passengers = List.of(new PassengerSummary(PassengerType.ADULT, 1));
		standardSeatIds = trainTestHelper.getSeatIds(train, CarType.STANDARD, 1);
	}

	@Test
	@DisplayName("동시에 같은 좌석에 여러 예약이 발생하면 1개의 예약만 성공한다.")
	void allowsOnlyOneReservationForConcurrentRequests() throws InterruptedException {
		// given
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();
		ScheduleStop departureStop = trainScheduleTestHelper.getScheduleStopByStationName(scheduleWithStops, "1");
		ScheduleStop arrivalStop = trainScheduleTestHelper.getScheduleStopByStationName(scheduleWithStops, "3");
		trainScheduleTestHelper.createOrUpdateStationFare("1", "3", 50000, 10000);
		var request = createRequest(scheduleWithStops, departureStop, arrivalStop, passengers, standardSeatIds);

		// when
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					reservationApplicationService.createReservation(request, memberNo);
					successCount.getAndIncrement();
				} catch (BusinessException e) {
					failCount.getAndIncrement();
				} finally {
					latch.countDown();
				}
			});
		}
		latch.await();
		executorService.shutdown();

		// then
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(threadCount - 1);
	}

	@Test
	@DisplayName("같은 좌석에 대해 겹치는 구간의 예약이 동시에 발생하면 1개의 예약만 성공한다.")
	void allowsOnlyOneReservationForOverlappingRoutesWithConcurrentRequests() throws InterruptedException {
		// given
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		ScheduleStop one = trainScheduleTestHelper.getScheduleStopByStationName(scheduleWithStops, "1");
		ScheduleStop three = trainScheduleTestHelper.getScheduleStopByStationName(scheduleWithStops, "3");
		trainScheduleTestHelper.createOrUpdateStationFare("1", "3", 50000, 10000);
		var oneToThreeRequest = createRequest(scheduleWithStops, one, three, passengers, standardSeatIds);

		ScheduleStop two = trainScheduleTestHelper.getScheduleStopByStationName(scheduleWithStops, "2");
		ScheduleStop four = trainScheduleTestHelper.getScheduleStopByStationName(scheduleWithStops, "4");
		trainScheduleTestHelper.createOrUpdateStationFare("2", "4", 50000, 10000);
		var twoToFourRequest = createRequest(scheduleWithStops, two, four, passengers, standardSeatIds);

		// when
		// 절반은 1->3 구간, 절반은 2->4 구간 예약 시도
		for (int i = 0; i < threadCount; i++) {
			final int index = i;
			executorService.submit(() -> {
				try {
					if (index % 2 == 0) {
						reservationApplicationService.createReservation(oneToThreeRequest, memberNo);
					} else {
						reservationApplicationService.createReservation(twoToFourRequest, memberNo);
					}
					successCount.getAndIncrement();
				} catch (BusinessException e) {
					failCount.getAndIncrement();
				} finally {
					latch.countDown();
				}
			});
		}
		latch.await();
		executorService.shutdown();

		// then
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(threadCount - 1);
	}

	private static ReservationCreateRequest createRequest(
		TrainScheduleTestHelper.TrainScheduleWithStopStations scheduleWithStops,
		ScheduleStop departureStop,
		ScheduleStop arrivalStop,
		List<PassengerSummary> passengers,
		List<Long> standardSeatIds
	) {
		return new ReservationCreateRequest(
			scheduleWithStops.trainSchedule().getId(),
			departureStop.getStation().getId(),
			arrivalStop.getStation().getId(),
			passengers,
			standardSeatIds,
			TripType.OW
		);
	}
}
