package com.sudo.railo.train.application.dto;

import java.time.LocalTime;

public record TrainBasicInfo(
	Long trainScheduleId,
	Integer trainNumber,
	String trainName,
	String departureStationName,
	String arrivalStationName,
	LocalTime departureTime,
	LocalTime arrivalTime
) {
}
