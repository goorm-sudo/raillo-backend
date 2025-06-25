package com.sudo.railo.booking.infra;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sudo.railo.booking.domain.SeatInventory;

public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {
	// 스케줄 ID와 좌석 ID로 좌석 예약 상태를 조회하는 메서드
	Optional<SeatInventory> findByTrainScheduleIdAndSeatId(Long trainScheduleId, Long seatId);
	
	// 스케줄 ID로 예약 가능한 좌석 목록을 조회하는 메서드
	@Query("SELECT si FROM SeatInventory si WHERE si.trainSchedule.id = :trainScheduleId AND si.seatStatus = 'AVAILABLE'")
	List<SeatInventory> findAvailableSeatsByTrainScheduleId(Long trainScheduleId);

	// 예약 만료 시간을 기준으로 만료된 좌석 목록을 조회하는 메서드
	@Query("SELECT si FROM SeatInventory si WHERE si.seatStatus = 'RESERVED' AND si.reservedAt < :expiredAt")
	List<SeatInventory> findExpiredSeats(LocalDateTime expiredAt);
}
