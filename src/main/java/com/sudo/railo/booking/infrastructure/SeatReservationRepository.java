package com.sudo.railo.booking.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.sudo.railo.booking.domain.SeatReservation;

import jakarta.persistence.LockModeType;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

	/***
	 * 스케줄 ID와 좌석 ID로 좌석 예약 상태를 조회하는 메서드
	 * 비관적 락을 사용하여 해당 열차 스케줄과 좌석의 모든 예약을 조회
	 * 동시성 제어를 위해 SeatReservation에 배타적 락을 걸어 다른 트랜잭션의 접근을 차단
	 * @param trainScheduleId 열차 스케줄 ID
	 * @param seatId 좌석 ID
	 * @return SeatReservation 엔티티
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT sr FROM SeatReservation sr WHERE sr.trainSchedule.id = :trainScheduleId AND sr.seat.id = :seatId")
	List<SeatReservation> findByTrainScheduleAndSeatWithLock(Long trainScheduleId, Long seatId);

	List<SeatReservation> findByReservationId(Long reservationId);

	void deleteAllByReservationId(Long reservationId);
}
