package com.sudo.railo.support.helper;

import static com.sudo.railo.support.helper.TrainScheduleTestHelper.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

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

import jakarta.transaction.Transactional;
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
	public Reservation createReservation(Member member, TrainScheduleWithStopStations scheduleWithStops) {
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
