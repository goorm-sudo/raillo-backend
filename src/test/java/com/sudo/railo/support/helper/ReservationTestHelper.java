package com.sudo.railo.support.helper;

import static com.sudo.railo.support.helper.TrainScheduleTestHelper.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.SeatReservation;
import com.sudo.railo.booking.domain.status.ReservationStatus;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.booking.domain.type.TripType;
import com.sudo.railo.booking.infrastructure.SeatReservationRepository;
import com.sudo.railo.booking.infrastructure.reservation.ReservationRepository;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.type.CarType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class ReservationTestHelper {

	private final TrainTestHelper trainTestHelper;
	private final ReservationRepository reservationRepository;
	private final SeatReservationRepository seatReservationRepository;

	/**
	 * 기본 예약 생성 메서드
	 */
	public Reservation createReservation(Member member,
		TrainScheduleWithStopStations scheduleWithStops) {
		Reservation reservation = Reservation.builder()
			.trainSchedule(scheduleWithStops.trainSchedule())
			.member(member)
			.reservationCode("20250806100001D49J")
			.tripType(TripType.OW)
			.totalPassengers(1)
			.passengerSummary("[{\"passengerType\":\"ADULT\",\"count\":1}]")
			.reservationStatus(ReservationStatus.RESERVED)
			.expiresAt(LocalDateTime.now().plusMinutes(10))
			.fare(50000)
			.departureStop(getDepartureStop(scheduleWithStops.scheduleStops()))
			.arrivalStop(getArrivalStop(scheduleWithStops.scheduleStops()))
			.build();

		reservationRepository.save(reservation);
		createSeatReservation(reservation);
		return reservation;
	}

	/**
	 * 특정 좌석 ID들에 대한 좌석 예약 생성 (출발, 도착 정차역 직접 지정)
	 *
	 * @param member 예약할 회원
	 * @param scheduleWithStops 열차 스케줄 및 정차역 정보
	 * @param departureStop 출발 정차역
	 * @param arrivalStop 도착 정차역
	 * @param seatIds 예약할 좌석 ID 목록
	 * @param passengerType 승객 유형 (성인, 어린이 등)
	 * @return 생성된 Reservation 객체
	 */
	public Reservation createReservationWithSeats(Member member,
		TrainScheduleWithStopStations scheduleWithStops,
		ScheduleStop departureStop,
		ScheduleStop arrivalStop,
		List<Long> seatIds,
		PassengerType passengerType) {

		Reservation reservation = Reservation.builder()
			.trainSchedule(scheduleWithStops.trainSchedule())
			.member(member)
			.reservationCode("SEAT-" + System.currentTimeMillis())
			.tripType(TripType.OW)
			.totalPassengers(seatIds.size())
			.passengerSummary("[{\"passengerType\":\"" + passengerType.name() + "\",\"count\":" + seatIds.size() + "}]")
			.reservationStatus(ReservationStatus.RESERVED)
			.expiresAt(LocalDateTime.now().plusMinutes(10))
			.fare(50000 * seatIds.size())
			.departureStop(departureStop)
			.arrivalStop(arrivalStop)
			.build();

		reservationRepository.save(reservation);
		createSeatReservations(reservation, seatIds, passengerType);
		return reservation;
	}

	/**
	 * 지정한 인원 수만큼 입석 예약 생성 (출발, 도착 정차역 직접 지정, seat 없이)
	 *
	 * @param member 예약할 회원
	 * @param scheduleWithStops 열차 스케줄 및 정차역 정보
	 * @param departureStop 출발 정차역
	 * @param arrivalStop 도착 정차역
	 * @param standingCount 입석 인원 수
	 * @return 생성된 Reservation 객체
	 */
	public Reservation createStandingReservation(Member member,
		TrainScheduleWithStopStations scheduleWithStops,
		ScheduleStop departureStop,
		ScheduleStop arrivalStop,
		int standingCount) {

		Reservation reservation = Reservation.builder()
			.trainSchedule(scheduleWithStops.trainSchedule())
			.member(member)
			.reservationCode("STANDING-" + System.currentTimeMillis())
			.tripType(TripType.OW)
			.totalPassengers(standingCount)
			.passengerSummary("[{\"passengerType\":\"ADULT\",\"count\":" + standingCount + "}]")
			.reservationStatus(ReservationStatus.RESERVED)
			.expiresAt(LocalDateTime.now().plusMinutes(10))
			.fare((int)(50000 * 0.85) * standingCount)
			.departureStop(departureStop)
			.arrivalStop(arrivalStop)
			.build();

		reservationRepository.save(reservation);
		createStandingSeatReservations(reservation, standingCount);
		return reservation;
	}

	/**
	 * 좌석 예약 생성 메서드
	 */
	private void createSeatReservation(Reservation reservation) {
		Train train = reservation.getTrainSchedule().getTrain();
		List<Seat> seats = trainTestHelper.getSeats(train, CarType.STANDARD, 1);

		List<SeatReservation> seatReservations = seats.stream()
			.map(seat -> SeatReservation.builder()
				.trainSchedule(reservation.getTrainSchedule())
				.seat(seat)
				.reservation(reservation)
				.passengerType(PassengerType.ADULT)
				.build())
			.toList();

		seatReservationRepository.saveAll(seatReservations);
	}

	/**
	 * 주어진 좌석 ID들로 SeatReservation 생성
	 */
	private void createSeatReservations(Reservation reservation, List<Long> seatIds, PassengerType passengerType) {
		List<Seat> seats = trainTestHelper.getSeatsByIds(seatIds);

		List<SeatReservation> seatReservations = seats.stream()
			.map(seat -> SeatReservation.builder()
				.trainSchedule(reservation.getTrainSchedule())
				.seat(seat)
				.reservation(reservation)
				.passengerType(passengerType)
				.build())
			.toList();

		seatReservationRepository.saveAll(seatReservations);
	}

	/**
	 * Seat=null로 승객수만큼 입석 SeatReservations 생성
	 */
	private void createStandingSeatReservations(Reservation reservation, int count) {
		List<SeatReservation> seatReservations =
			java.util.stream.IntStream.range(0, count)
				.mapToObj(i -> SeatReservation.builder()
					.trainSchedule(reservation.getTrainSchedule())
					.seat(null) // 입석 처리
					.reservation(reservation)
					.passengerType(PassengerType.ADULT)
					.build())
				.toList();

		seatReservationRepository.saveAll(seatReservations);
	}

	private ScheduleStop getDepartureStop(List<ScheduleStop> scheduleStops) {
		if (scheduleStops.isEmpty()) {
			throw new IllegalArgumentException("출발역을 찾을 수 없습니다.");
		}
		return scheduleStops.get(0);
	}

	private ScheduleStop getArrivalStop(List<ScheduleStop> scheduleStops) {
		if (scheduleStops.isEmpty()) {
			throw new IllegalArgumentException("도착역을 찾을 수 없습니다.");
		}
		return scheduleStops.get(scheduleStops.size() - 1);
	}
}
