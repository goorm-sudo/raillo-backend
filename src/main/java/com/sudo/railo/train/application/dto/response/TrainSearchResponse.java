package com.sudo.railo.train.application.dto.response;

import java.time.Duration;
import java.time.LocalTime;

import com.sudo.railo.train.domain.type.SeatAvailabilityStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "열차 검색 응답")
public record TrainSearchResponse(
	@Schema(description = "열차 번호", example = "027")
	String trainNumber,

	@Schema(description = "열차명", example = "KTX")
	String trainName,

	@Schema(description = "출발 시간", example = "11:58")
	LocalTime departureTime,

	@Schema(description = "도착 시간", example = "12:38")
	LocalTime arrivalTime,

	@Schema(description = "소요 시간", example = "PT40M")
	Duration travelTime,

	@Schema(description = "일반실 정보")
	SeatTypeInfo standardSeat,

	@Schema(description = "특실 정보")
	SeatTypeInfo firstClassSeat,

	@Schema(description = "입석 정보 (있는 경우)")
	StandingTypeInfo standing,

	@Schema(description = "전체 상태", example = "AVAILABLE")
	SeatAvailabilityStatus overallStatus
) {
	public TrainSearchResponse {
		// 1. Validation
		if (trainNumber == null || trainNumber.isBlank()) {
			throw new IllegalArgumentException("열차번호는 필수입니다");
		}
		if (trainName == null || trainName.isBlank()) {
			throw new IllegalArgumentException("열차명은 필수입니다");
		}
		if (departureTime == null) {
			throw new IllegalArgumentException("출발시간은 필수입니다");
		}
		if (arrivalTime == null) {
			throw new IllegalArgumentException("도착시간은 필수입니다");
		}
		if (standardSeat == null) {
			throw new IllegalArgumentException("일반실 정보는 필수입니다");
		}
		if (firstClassSeat == null) {
			throw new IllegalArgumentException("특실 정보는 필수입니다");
		}
		if (overallStatus == null) {
			throw new IllegalArgumentException("전체 상태는 필수입니다");
		}

		// 2. 자동 계산 - travelTime이 null이면 자동으로 계산
		if (travelTime == null) {
			travelTime = Duration.between(departureTime, arrivalTime);
		}

		// 3. 도착시간 validation
		if (arrivalTime.isBefore(departureTime)) {
			throw new IllegalArgumentException("도착시간은 출발시간보다 늦어야 합니다");
		}
	}

	/* 정적 팩토리 메서드 */
	public static TrainSearchResponse seatsOnly(String trainNumber, String trainName,
		LocalTime departureTime, LocalTime arrivalTime,
		SeatTypeInfo standardSeat, SeatTypeInfo firstClassSeat,
		SeatAvailabilityStatus overallStatus) {
		return new TrainSearchResponse(
			trainNumber, trainName, departureTime, arrivalTime,
			null, // travelTime을 null로 전달하면 Compact Constructor에서 자동 계산
			standardSeat, firstClassSeat, null, overallStatus
		);
	}

	public static TrainSearchResponse withStanding(String trainNumber, String trainName,
		LocalTime departureTime, LocalTime arrivalTime,
		SeatTypeInfo standardSeat, SeatTypeInfo firstClassSeat,
		StandingTypeInfo standing, SeatAvailabilityStatus overallStatus) {
		return new TrainSearchResponse(
			trainNumber, trainName, departureTime, arrivalTime,
			null, // travelTime을 null로 전달하면 Compact Constructor에서 자동 계산
			standardSeat, firstClassSeat, standing, overallStatus
		);
	}

	/* 편의 메서드 */
	public boolean hasStanding() {
		return standing != null;
	}

	public boolean isExpressTrain() {
		return trainName.contains("KTX");
	}

	public String getFormattedTravelTime() {
		long minutes = travelTime.toMinutes();
		long hours = minutes / 60;
		long remainingMinutes = minutes % 60;

		if (hours > 0) {
			return String.format("%d시간 %d분", hours, remainingMinutes);
		} else {
			return String.format("%d분", remainingMinutes);
		}
	}
}
