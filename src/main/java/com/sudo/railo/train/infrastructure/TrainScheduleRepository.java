package com.sudo.railo.train.infrastructure;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sudo.railo.train.domain.TrainSchedule;

public interface TrainScheduleRepository extends JpaRepository<TrainSchedule, Long> {

	@Query("SELECT MAX(ts.operationDate) FROM TrainSchedule ts")
	Optional<LocalDate> findLastOperationDate();

	boolean existsByOperationDate(LocalDate operationDate);
	
	/**
	 * 마일리지 처리가 필요한 도착한 열차 조회
	 * @param currentTime 현재 시간
	 * @return 마일리지 미처리 도착 열차 목록
	 */
	@Query("SELECT ts FROM TrainSchedule ts " +
	       "WHERE ts.actualArrivalTime <= :currentTime " +
	       "AND ts.mileageProcessed = false " +
	       "ORDER BY ts.actualArrivalTime ASC")
	List<TrainSchedule> findArrivedTrainsForMileageProcessing(
	        @Param("currentTime") LocalDateTime currentTime);
	
	/**
	 * 마일리지 처리 완료 표시
	 * @param trainScheduleId 열차 스케줄 ID
	 */
	@Modifying
	@Query("UPDATE TrainSchedule ts SET ts.mileageProcessed = true WHERE ts.id = :trainScheduleId")
	void markMileageProcessed(@Param("trainScheduleId") Long trainScheduleId);
}
