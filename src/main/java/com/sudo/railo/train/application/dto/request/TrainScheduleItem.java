package com.sudo.railo.train.application.dto.request;

import java.time.LocalTime;
import java.util.List;

import com.sudo.railo.train.domain.status.OperationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record TrainScheduleItem(

	@Schema(description = "열차 스케줄 ID", example = "1")
	Long scheduleId,

	@Schema(description = "열차 번호", example = "413")
	Integer trainNumber,

	@Schema(description = "열차 종류", example = "KTX")
	String trainType,

	@Schema(description = "열차 이름", example = "KTX-산천")
	String trainName,

	@Schema(description = "출발 시간", example = "11:32")
	LocalTime departureTime,

	@Schema(description = "도착 시간", example = "KTX")
	LocalTime arrivalTime,

	@Schema(description = "열차 종류", example = "KTX")
	Long travelTimeMinutes,

	@Schema(description = "열차 종류", example = "KTX")
	OperationStatus operationStatus,

	@Schema(description = "좌석 가용성 정보")
	List<SeatAvailabilityItem> seatAvailabilityItems
) {
}
