package com.sudo.railo.train.application.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrainScheduleDto {

	private String scheduleName;
	private LocalDate operationDate;
	private List<ScheduleStopDto> scheduleStopDtos;
	private TrainDto trainDto;

	public static TrainScheduleDto of(String scheduleName, LocalDate operationDate,
		List<ScheduleStopDto> scheduleStopDtos, TrainDto trainDto) {

		return new TrainScheduleDto(scheduleName, operationDate, scheduleStopDtos, trainDto);
	}

	public ScheduleStopDto getFirstStop() {
		if (scheduleStopDtos == null || scheduleStopDtos.isEmpty()) {
			throw new IllegalStateException("스케줄 정차역 정보가 비어 있습니다.");
		}
		return scheduleStopDtos.get(0);
	}

	public ScheduleStopDto getLastStop() {
		if (scheduleStopDtos == null || scheduleStopDtos.isEmpty()) {
			throw new IllegalStateException("스케줄 정차역 정보가 비어 있습니다.");
		}
		return scheduleStopDtos.get(scheduleStopDtos.size() - 1);
	}
}
