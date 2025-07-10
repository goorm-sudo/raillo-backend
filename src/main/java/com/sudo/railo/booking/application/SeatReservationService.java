package com.sudo.railo.booking.application;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sudo.railo.booking.domain.PassengerType;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.SeatReservation;
import com.sudo.railo.booking.domain.SeatStatus;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infra.SeatReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.infrastructure.SeatRepository;

import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

	private final SeatRepository seatRepository;
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
			SeatStatus seatStatus = SeatStatus.RESERVED;
			LocalDateTime reservedAt = LocalDateTime.now();
			SeatReservation seatReservation = SeatReservation.builder()
				.trainSchedule(reservation.getTrainSchedule())
				.seat(seat)
				.reservation(reservation)
				.passengerType(passengerType)
				.seatStatus(seatStatus)
				.reservedAt(reservedAt)
				.departureStation(reservation.getDepartureStation())
				.arrivalStation(reservation.getArrivalStation())
				.build();
			return seatReservationRepository.save(seatReservation);
		} catch (OptimisticLockException | DataIntegrityViolationException e) {
			// 동시성 문제 및 유니크 제약 위반 발생
			throw new BusinessException(BookingError.SEAT_ALREADY_RESERVED);
		} catch (Exception e) {
			// 알 수 없는 모든 경우는 실패 처리
			throw new BusinessException(BookingError.SEAT_RESERVATION_FAILED);
		}
	}
}
