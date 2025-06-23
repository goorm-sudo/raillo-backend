package com.sudo.railo.train.application.dto.response;

import java.time.Duration;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "정차역 정보")
public record StationStopInfo(
	@Schema(description = "역 ID")
	Long stationId,

	@Schema(description = "역명", example = "서울")
	String stationName,

	@Schema(description = "정차 순서", example = "1")
	int stopOrder,

	@Schema(description = "도착 시간", example = "09:00")
	LocalTime arrivalTime,

	@Schema(description = "출발 시간", example = "09:03")
	LocalTime departureTime
) {
	public static StationStopInfo of(Long stationId, String stationName, int stopOrder,
		LocalTime arrivalTime, LocalTime departureTime) {
		return new StationStopInfo(stationId, stationName, stopOrder, arrivalTime, departureTime);
	}

	/**
	 * 정차 시간(분) 계산
	 */
	public int getStopDurationMinutes() {
		if (arrivalTime != null && departureTime != null) {
			return (int)Duration.between(arrivalTime, departureTime).toMinutes();
		}
		return 0;
	}
}
