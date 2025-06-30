package com.sudo.railo.booking.infra;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.booking.domain.SeatReservation;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

	/***
	 * 스케줄 ID와 좌석 ID로 좌석 예약 상태를 조회하는 메서드
	 * @param trainScheduleId 열차 스케줄 ID
	 * @param seatId 좌석 ID
	 * @return SeatReservation 엔티티
	 */
	Optional<SeatReservation> findByTrainScheduleIdAndSeatId(Long trainScheduleId, Long seatId);

	/***
	 * 예약 만료 시간을 기준으로 만료된 좌석 목록을 조회하는 메서드
	 * @param reservedAtBefore 만료 기준 시간
	 * @return 만료된 SeatReservation 엔티티 리스트
	 */
	List<SeatReservation> findAllByReservedAtBefore(LocalDateTime reservedAtBefore);
}
