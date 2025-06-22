package com.sudo.railo.train.application.dto;

import java.time.LocalTime;

import com.sudo.railo.train.domain.Station;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleStopDto {
	private int stopOrder;
	private LocalTime arrivalTime;
	private LocalTime departureTime;
	private Station station;

	public static ScheduleStopDto of(int stopOrder, LocalTime arrivalTime, LocalTime departureTime, Station station) {
		return new ScheduleStopDto(stopOrder, arrivalTime, departureTime, station);
	}

	public static ScheduleStopDto first(ScheduleStopDto scheduleStopDto) {
		return new ScheduleStopDto(
			scheduleStopDto.getStopOrder(),
			null,
			scheduleStopDto.getDepartureTime(),
			scheduleStopDto.getStation()
		);
	}

	public static ScheduleStopDto last(ScheduleStopDto scheduleStopDto) {
		return new ScheduleStopDto(
			scheduleStopDto.getStopOrder(),
			scheduleStopDto.getDepartureTime(),
			null,
			scheduleStopDto.getStation()
		);
	}
}
