package com.sudo.railo.booking.application;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.sudo.railo.booking.application.dto.request.ReservationCreateRequest;
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
	 * @param request 예약 생성 요청 DTO
	 * @return SeatReservation Entity
	 */
	@Transactional
	public SeatReservation reserveNewSeat(Reservation reservation, ReservationCreateRequest request) {
		try {
			Seat seat = seatRepository.findById(request.getSeatId())
				.orElseThrow(() -> new BusinessException(BookingError.SEAT_NOT_FOUND));
			SeatStatus seatStatus = SeatStatus.RESERVED;
			LocalDateTime reservedAt = LocalDateTime.now();
			SeatReservation seatReservation = SeatReservation.builder()
				.trainSchedule(reservation.getTrainSchedule())
				.seat(seat)
				.reservation(reservation)
				.seatStatus(seatStatus)
				.reservedAt(reservedAt)
				.departureStation(reservation.getDepartureStation())
				.arrivalStation(reservation.getArrivalStation())
				.build();
			return seatReservationRepository.save(seatReservation);
		} catch (OptimisticLockException e) {
			// 동시성 문제 발생
			throw new BusinessException(BookingError.SEAT_ALREADY_RESERVED);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			throw new BusinessException(BookingError.SEAT_RESERVATION_FAILED);
		}
	}

	/***
	 * 이미 존재하는 좌석 예약 현황을 예약하는 메서드
	 * @deprecated 예약 현황을 미리 만들어두는게 아님.
	 * @param trainScheduleId 열차 스케줄 ID
	 * @param seatId 좌석 ID
	 * @return SeatReservation Entity
	 */
	@Transactional
	public SeatReservation reserveExistingSeat(Long trainScheduleId, Long seatId) {
		try {
			SeatReservation seatReservation = seatReservationRepository
				.findByTrainScheduleIdAndSeatId(trainScheduleId, seatId)
				.orElseThrow(() -> new BusinessException(BookingError.SEAT_NOT_FOUND));
			seatReservation.reserveSeat();
			return seatReservationRepository.save(seatReservation);
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
	 * @deprecated 취소 대신 아예 삭제하는게 나아보입니다.
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
}
