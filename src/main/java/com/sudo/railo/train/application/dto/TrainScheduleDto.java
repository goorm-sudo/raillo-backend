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
	private List<ScheduleStopDto> scheduleStopsDto;
	private TrainDto trainDto;

	public static TrainScheduleDto of(String scheduleName, LocalDate operationDate,
		List<ScheduleStopDto> scheduleStopsDto, TrainDto trainDto) {

		return new TrainScheduleDto(scheduleName, operationDate, scheduleStopsDto, trainDto);
	}

	public ScheduleStopDto getFirstStop() {
		if (scheduleStopsDto == null || scheduleStopsDto.isEmpty()) {
			throw new IllegalStateException("스케줄 정차역 정보가 비어 있습니다.");
		}
		return scheduleStopsDto.get(0);
	}

	public ScheduleStopDto getLastStop() {
		if (scheduleStopsDto == null || scheduleStopsDto.isEmpty()) {
			throw new IllegalStateException("스케줄 정차역 정보가 비어 있습니다.");
		}
		return scheduleStopsDto.get(scheduleStopsDto.size() - 1);
	}
}
