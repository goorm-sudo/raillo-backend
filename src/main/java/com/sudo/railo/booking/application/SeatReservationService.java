package com.sudo.railo.booking.application;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.SeatReservation;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infrastructure.SeatReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.domain.Seat;

import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

	private final SeatReservationRepository seatReservationRepository;

	/***
	 * 새로운 좌석 예약 현황을 생성하고 예약하는 메서드
	 * @param reservation Reservation Entity
	 * @param seat Seat Entity
	 * @return SeatReservation Entity
	 */
	@Transactional
	public SeatReservation reserveNewSeat(Reservation reservation, Seat seat, PassengerType passengerType) {
		try {
			Long trainScheduleId = reservation.getTrainSchedule().getId();
			Long seatId = seat.getId();

			// 비관적 락으로 해당 좌석의 모든 예약을 조회 (다른 트랜잭션의 접근 차단)
			List<SeatReservation> existingReservations = seatReservationRepository
				.findByTrainScheduleAndSeatWithLock(trainScheduleId, seatId);

			// 락이 걸린 상태에서 충돌 검증 (원자성 보장)
			validateConflictWithExistingReservations(reservation, existingReservations);

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

	/**
	 * 기존 예약들과 충돌 검증 (락이 걸린 상태에서 수행)
	 */
	private void validateConflictWithExistingReservations(
		Reservation newReservation,
		List<SeatReservation> existingReservations
	) {
		int newDepartureOrder = newReservation.getDepartureStop().getStopOrder();
		int newArrivalOrder = newReservation.getArrivalStop().getStopOrder();

		existingReservations.forEach(existingReservation -> {
			int existingDepartureOrder = existingReservation.getReservation().getDepartureStop().getStopOrder();
			int existingArrivalOrder = existingReservation.getReservation().getArrivalStop().getStopOrder();
			if (existingDepartureOrder < newArrivalOrder && existingArrivalOrder > newDepartureOrder) {
				throw new BusinessException(BookingError.SEAT_ALREADY_RESERVED);
			}
		});
	}

	public void deleteSeatReservation(Long seatReservationId) {
		SeatReservation seatReservation = seatReservationRepository.findById(seatReservationId)
			.orElseThrow(() -> new BusinessException(BookingError.SEAT_RESERVATION_NOT_FOUND));
		seatReservationRepository.delete(seatReservation);
	}

	public void deleteSeatReservationByReservationId(Long reservationId) {
		seatReservationRepository.deleteAllByReservationId(reservationId);
	}
}
