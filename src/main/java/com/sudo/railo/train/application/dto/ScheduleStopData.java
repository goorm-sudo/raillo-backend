package com.sudo.railo.train.application.dto;

import java.time.LocalTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleStopData {

	private int stopOrder;
	private LocalTime arrivalTime;
	private LocalTime departureTime;
	private String stationName;

	/**
	 * 정적 팩토리 메서드
	 *
	 * @param stopOrder 정차 순서
	 * @param arrivalTime 도착 시간
	 * @param departureTime 출발 시간
	 * @param stationName 역 이름
	 */
	public static ScheduleStopData of(int stopOrder, LocalTime arrivalTime, LocalTime departureTime,
		String stationName) {

		return new ScheduleStopData(stopOrder, arrivalTime, departureTime, stationName);
	}

	/**
	 * 도착 시간이 없는 출발역 생성
	 */
	public static ScheduleStopData first(ScheduleStopData data) {
		return new ScheduleStopData(
			data.getStopOrder(),
			null,
			data.getDepartureTime(),
			data.getStationName()
		);
	}

	/**
	 * 출발 시간이 없는 마지막 정차역 생성
	 */
	public static ScheduleStopData last(ScheduleStopData data) {
		return new ScheduleStopData(
			data.getStopOrder(),
			data.getDepartureTime(),
			null,
			data.getStationName()
		);
	}
}
