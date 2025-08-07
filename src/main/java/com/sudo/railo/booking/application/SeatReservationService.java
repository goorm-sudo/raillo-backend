package com.sudo.railo.booking.application;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.SeatReservation;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infrastructure.SeatReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.infrastructure.SeatRepository;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

	private final SeatReservationRepository seatReservationRepository;
	private final SeatRepository seatRepository;

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

			// 1. 먼저 좌석 자체에 비관적 락을 걸어 동시 접근 차단 (최우선 락)
			Seat lockedSeat = seatRepository.findByIdWithLock(seatId)
				.orElseThrow(() -> new BusinessException(BookingError.SEAT_NOT_FOUND));

			// 2. 락이 걸린 상태에서 해당 좌석의 기존 예약들을 비관적 락으로 조회
			List<SeatReservation> existingReservations = seatReservationRepository
				.findByTrainScheduleAndSeatWithLock(trainScheduleId, seatId);

			// 3. 락이 걸린 상태에서 충돌 검증 (원자성 보장)
			validateConflictWithExistingReservations(reservation, existingReservations);

			SeatReservation seatReservation = SeatReservation.builder()
				.trainSchedule(reservation.getTrainSchedule())
				.seat(lockedSeat)
				.reservation(reservation)
				.passengerType(passengerType)
				.build();
			return seatReservationRepository.save(seatReservation);
		} catch (OptimisticLockException | DataIntegrityViolationException e) {
			// 동시성 문제 및 유니크 제약 위반 발생
			throw new BusinessException(BookingError.SEAT_ALREADY_RESERVED);
		}
	}

	@Transactional
	public void deleteSeatReservation(Long seatReservationId) {
		SeatReservation seatReservation = seatReservationRepository.findById(seatReservationId)
			.orElseThrow(() -> new BusinessException(BookingError.SEAT_RESERVATION_NOT_FOUND));
		seatReservationRepository.delete(seatReservation);
	}

	@Transactional
	public void deleteSeatReservationByReservationId(Long reservationId) {
		seatReservationRepository.deleteAllByReservationId(reservationId);
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
}
