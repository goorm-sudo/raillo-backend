package com.sudo.railo.train.application.dto;

import java.time.LocalTime;

public record TrainBasicInfo(
	Long trainScheduleId,
	Integer trainNumber,
	String trainName,
	LocalTime departureTime,
	LocalTime arrivalTime
) {
}
