package com.sudo.railo.booking.infra;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.sudo.railo.booking.domain.SeatReservation;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {
	// 스케줄 ID와 좌석 ID로 좌석 예약 상태를 조회하는 메서드
	Optional<SeatReservation> findByTrainScheduleIdAndSeatId(Long trainScheduleId, Long seatId);

	// 스케줄 ID로 예약 가능한 좌석 목록을 조회하는 메서드
	@Query("SELECT sr FROM SeatReservation sr WHERE sr.trainSchedule.id = :trainScheduleId AND sr.seatStatus = 'AVAILABLE'")
	List<SeatReservation> findAvailableSeatsByTrainScheduleId(Long trainScheduleId);

	// 예약 만료 시간을 기준으로 만료된 좌석 목록을 조회하는 메서드
	@Query("SELECT sr FROM SeatReservation sr WHERE sr.seatStatus = 'RESERVED' AND sr.reservedAt < :expiredAt")
	List<SeatReservation> findExpiredSeats(LocalDateTime expiredAt);

	// 예약 만료 시간을 기준으로 만료된 좌석을 취소하는 메서드
	@Modifying(clearAutomatically = true)
	@Query("UPDATE SeatReservation sr SET sr.seatStatus = 'AVAILABLE', sr.reservedAt = null WHERE sr.seatStatus = 'RESERVED' AND sr.reservedAt < :expiredAt")
	void cancelExpiredSeats(LocalDateTime expiredAt);
}
