package com.sudo.railo.train.infrastructure;

import java.time.LocalDate;
import java.util.Set;

public interface TrainScheduleRepositoryCustom {

	/**
	 * 날짜 범위에서 활성 스케줄이 있는 날짜들 조회 (운행 스케줄 캘린더 조회)
	 * TODO : 성능 모니터링 필요 : operation_calender 테이블, 배치, 캐시로 성능 개선 예정
	 */
	Set<LocalDate> findDatesWithActiveSchedules(LocalDate startDate, LocalDate endDate);
}
