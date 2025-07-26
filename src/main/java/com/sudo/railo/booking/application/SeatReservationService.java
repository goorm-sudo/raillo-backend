package com.sudo.railo.booking.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.SeatReservation;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infrastructure.SeatReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.infrastructure.SeatReservationRepositoryCustom;

import java.util.List;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

	private final SeatReservationRepository seatReservationRepository;
	private final SeatReservationRepositoryCustom seatReservationRepositoryCustom;

	/***
	 * 새로운 좌석 예약 현황을 생성하고 예약하는 메서드
	 * @param reservation Reservation Entity
	 * @param seat Seat Entity
	 * @return SeatReservation Entity
	 */
	@Transactional
	public SeatReservation reserveNewSeat(Reservation reservation, Seat seat, PassengerType passengerType) {
		try {
			validateConflictSeats(reservation, List.of(seat.getId()));
			SeatReservation seatReservation = SeatReservation.builder()
				.trainSchedule(reservation.getTrainSchedule())
				.seat(seat)
				.reservation(reservation)
				.passengerType(passengerType)
				.build();
			return seatReservationRepository.save(seatReservation);
		} catch (OptimisticLockException | DataIntegrityViolationException e) {
			// 동시성 문제 및 유니크 제약 위반 발생
			throw new BusinessException(BookingError.SEAT_ALREADY_RESERVED);
		}
	}

	private void validateConflictSeats(Reservation reservation, List<Long> seatIds) {
		Long trainScheduleId = reservation.getTrainSchedule().getId();
		Long departureStationId = reservation.getDepartureStop().getStation().getId();
		Long arrivalStationId = reservation.getArrivalStop().getStation().getId();

		seatIds.forEach(seatId -> {
			boolean isAvailable = seatReservationRepositoryCustom.isSeatAvailableForSection(
				trainScheduleId, seatId, departureStationId, arrivalStationId
			);
			if (!isAvailable) {
				throw new BusinessException(BookingError.SEAT_ALREADY_RESERVED);
			}
		});
	}
}
