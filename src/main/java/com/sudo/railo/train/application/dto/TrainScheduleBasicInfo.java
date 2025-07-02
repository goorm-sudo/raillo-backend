package com.sudo.railo.train.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 열차 스케줄 기본 정보 DTO
 */
public record TrainScheduleBasicInfo(
	@Schema(description = "열차 스케줄 ID", example = "1")
	Long scheduleId,

	@Schema(description = "열차 분류 코드 (KTX, SRT, ITX 등)", example = "KTX")
	String trainClassificationCode,

	@Schema(description = "열차 번호 (3자리 zero-padding)", example = "009")
	String trainNumber,

	@Schema(description = "열차명", example = "KTX")
	String trainName
) {
	public static TrainScheduleBasicInfo of(Long scheduleId, String trainClassificationCode,
		String trainNumber, String trainName) {
		return new TrainScheduleBasicInfo(scheduleId, trainClassificationCode, trainNumber, trainName);
	}
}
