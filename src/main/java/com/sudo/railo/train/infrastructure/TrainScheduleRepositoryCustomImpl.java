package com.sudo.railo.train.infrastructure;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.train.domain.QTrainSchedule;
import com.sudo.railo.train.domain.status.OperationStatus;

import lombok.RequiredArgsConstructor;

/**
 * 열차 스케줄 커스텀 Repository 구현체
 * QueryDSL을 활용한 복잡한 쿼리 및 성능이 중요한 복잡한 조회 로직
 */
@Repository
@RequiredArgsConstructor
public class TrainScheduleRepositoryCustomImpl implements TrainScheduleRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	/**
	 * 날짜 범위에서 활성 스케줄이 있는 날짜들 조회 (운행 스케줄 캘린더 조회)
	 * TODO : 성능 모니터링 필요 : operation_calender 테이블, 배치, 캐시로 성능 개선 예정
	 */
	@Override
	public Set<LocalDate> findDatesWithActiveSchedules(LocalDate startDate, LocalDate endDate) {
		QTrainSchedule trainSchedule = QTrainSchedule.trainSchedule;

		List<LocalDate> dates = queryFactory
			.select(trainSchedule.operationDate)
			.distinct()
			.from(trainSchedule)
			.where(
				trainSchedule.operationDate.between(startDate, endDate)
					.and(trainSchedule.operationStatus.in(OperationStatus.ACTIVE, OperationStatus.DELAYED))
			)
			.orderBy(trainSchedule.operationDate.asc())
			.fetch();

		return new LinkedHashSet<>(dates);
	}
}
