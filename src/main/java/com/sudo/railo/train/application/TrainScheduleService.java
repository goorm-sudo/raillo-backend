package com.sudo.railo.train.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.train.application.dto.request.OperationCalendarItem;
import com.sudo.railo.train.infrastructure.TrainScheduleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TrainScheduleService {

	private final TrainScheduleRepository trainScheduleRepository;

	public List<OperationCalendarItem> getOperationCalendar() {
		LocalDate startDate = LocalDate.now();
		LocalDate endDate = startDate.plusMonths(1);

		// 운행 날짜 조회 (Set으로 반환)
		Set<LocalDate> datesWithSchedule = trainScheduleRepository.findDatesWithActiveSchedules(startDate, endDate);

		List<OperationCalendarItem> calendar = startDate.datesUntil(endDate.plusDays(1))
			.map(date -> {
				boolean isHoliday = isHoliday(date);
				boolean hasSchedule = datesWithSchedule.contains(date);
				return OperationCalendarItem.create(date, isHoliday, hasSchedule);
			})
			.toList();

		log.info("운행 캘린더 조회 완료: {} ~ {} ({} 일), 운행일수: {}",
			startDate, endDate, calendar.size(), datesWithSchedule.size());

		return calendar;
	}

	/**
	 * 휴일 여부 판단
	 * TODO : 공휴일 API 연동 후 판단 처리 로직 추가 필요
	 */
	private boolean isHoliday(LocalDate date) {
		DayOfWeek dayOfWeek = date.getDayOfWeek();
		return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
	}
}
