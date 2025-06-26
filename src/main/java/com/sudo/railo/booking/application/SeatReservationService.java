package com.sudo.railo.booking.application;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.sudo.railo.booking.config.BookingConfig;
import com.sudo.railo.booking.domain.SeatReservation;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infra.SeatReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;

import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

	private final SeatReservationRepository seatReservationRepository;
	private final BookingConfig bookingConfig;

	/***
	 * 좌석을 예약하는 메서드
	 * @param trainScheduleId - 열차 스케줄 ID
	 * @param seatId - 좌석 ID
	 */
	@Transactional
	public void reserveSeat(Long trainScheduleId, Long seatId) {
		try {
			SeatReservation seatReservation = seatReservationRepository.findByTrainScheduleIdAndSeatId(trainScheduleId,
					seatId)
				.orElseThrow(() -> new BusinessException(BookingError.SEAT_NOT_FOUND));
			seatReservation.reserveSeat();
			seatReservationRepository.save(seatReservation);
		} catch (OptimisticLockException e) {
			// 동시성 문제 발생
			throw new BusinessException(BookingError.SEAT_ALREADY_RESERVED);
		} catch (BusinessException e) {
			// BusinessException은 그대로 넘기기
			throw e;
		} catch (Exception e) {
			throw new BusinessException(BookingError.SEAT_RESERVATION_FAILED);
		}
	}

	/***
	 * 좌석 예약을 취소하는 메서드
	 * @param trainScheduleId - 열차 스케줄 ID
	 * @param seatId - 좌석 ID
	 */
	@Transactional
	public void cancelReservation(Long trainScheduleId, Long seatId) {
		try {
			SeatReservation seatReservation = seatReservationRepository.findByTrainScheduleIdAndSeatId(trainScheduleId,
					seatId)
				.orElseThrow(() -> new BusinessException(BookingError.SEAT_NOT_FOUND));
			seatReservation.cancelReservation();
			seatReservationRepository.save(seatReservation);
		} catch (OptimisticLockException e) {
			// 동시성 문제 발생
			throw new BusinessException(BookingError.SEAT_ALREADY_CANCELLED);
		} catch (BusinessException e) {
			// BusinessException은 그대로 넘기기
			throw e;
		} catch (Exception e) {
			throw new BusinessException(BookingError.SEAT_CANCELLATION_FAILED);
		}
	}

	/***
	 * 예약 만료 시간을 기준으로 만료된 좌석을 취소하는 메서드
	 */
	@Transactional
	public void cancelExpiredReservation() {
		LocalDateTime expiredAt = LocalDateTime.now().minusMinutes(bookingConfig.getExpiration().getReservation());
		try {
			seatReservationRepository.cancelExpiredSeats(expiredAt);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			throw new BusinessException(BookingError.EXPIRED_SEAT_CANCELLATION_FAILED);
		}
	}
}
