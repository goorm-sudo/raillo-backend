package com.sudo.railo.train.application.dto.response;

import java.time.Duration;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "열차 검색 응답")
public record TrainSearchResponse(

	@Schema(description = "열차 스케줄 ID", example = "26")
	Long trainScheduleId,

	@Schema(description = "열차 번호", example = "027")
	String trainNumber,

	@Schema(description = "열차명", example = "KTX")
	String trainName,

	@Schema(description = "출발역명", example = "서울")
	String departureStationName,

	@Schema(description = "도착역명", example = "천안아산")
	String arrivalStationName,

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
	StandingTypeInfo standing
) {
	public TrainSearchResponse {
		if (travelTime == null) {
			// 자정을 지난 도착일 경우 다음날로 간주
			if (arrivalTime.isBefore(departureTime)) {
				travelTime = Duration.between(departureTime, arrivalTime.plusHours(24));
			} else {
				travelTime = Duration.between(departureTime, arrivalTime);
			}
		}
	}

	/* 정적 팩토리 메서드 */

	/**
	 * 생성 메서드
	 */
	public static TrainSearchResponse of(Long trainScheduleId, String trainNumber, String trainName,
		String departureStationName, String arrivalStationName,
		LocalTime departureTime, LocalTime arrivalTime,
		SeatTypeInfo standardSeat, SeatTypeInfo firstClassSeat,
		StandingTypeInfo standing) {

		validateTrainSearchData(trainScheduleId, trainNumber, trainName, departureTime, arrivalTime, standardSeat,
			firstClassSeat);

		return new TrainSearchResponse(
			trainScheduleId,
			trainNumber, trainName,
			departureStationName, arrivalStationName,
			departureTime, arrivalTime,
			null, // travelTime은 자동 계산
			standardSeat, firstClassSeat, standing
		);
	}

	private static void validateTrainSearchData(Long trainScheduleId, String trainNumber, String trainName,
		LocalTime departureTime, LocalTime arrivalTime,
		SeatTypeInfo standardSeat, SeatTypeInfo firstClassSeat) {
		if (trainScheduleId == null) {
			throw new IllegalArgumentException("열차 스케줄 ID는 필수입니다");
		}
		if (trainNumber == null || trainNumber.isBlank()) {
			throw new IllegalArgumentException("열차번호는 필수입니다");
		}
		if (departureTime == null || arrivalTime == null) {
			throw new IllegalArgumentException("출발/도착시간은 필수입니다");
		}
		if (standardSeat == null || firstClassSeat == null) {
			throw new IllegalArgumentException("좌석 정보는 필수입니다");
		}
	}

	/* 편의 메서드 */

	/**
	 * 입석 정보 존재 여부
	 */
	public boolean hasStanding() {
		return standing != null;
	}

	/**
	 * 고속 열차(KTX/SRT) 여부
	 */
	public boolean isExpressTrain() {
		return trainName.contains("KTX");
	}

	/**
	 * 소요 시간 포맷팅
	 */
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
