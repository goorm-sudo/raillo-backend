package com.sudo.railo.train.infrastructure;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sudo.railo.train.domain.TrainSchedule;

public interface TrainScheduleRepository extends JpaRepository<TrainSchedule, Long> {

	/**
	 * 날짜 범위에서 활성 스케줄이 있는 날짜들 조회 (운행 스케줄 캘린더 조회)
	 * TODO : 성능 모니터링 필요 : operation_calender 테이블, 배치, 캐시로 성능 개선 예정
	 */
	@Query("""
		SELECT DISTINCT ts.operationDate
		        FROM TrainSchedule ts
		        WHERE ts.operationDate BETWEEN :startDate AND :endDate
		        AND ts.operationStatus IN ('ACTIVE', 'DELAYED')
		        ORDER BY ts.operationDate
		""")
	Set<LocalDate> findDatesWithActiveSchedules(
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate);
}
