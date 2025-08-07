package com.sudo.railo.train.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 열차 객차 목록 조회 응답
 */
@Schema(description = "열차 객차 목록 조회 응답")
public record TrainCarListResponse(

	@Schema(description = "열차 스케줄 ID", example = "26")
	Long trainScheduleId,

	@Schema(description = "AI가 추천하는 최적 객차 번호 (승객수, 위치 고려)", example = "14")
	String recommendedCarNumber,

	@Schema(description = "잔여 좌석이 있는 전체 객차 수", example = "18")
	int totalCarCount,

	@Schema(description = "열차 분류 코드", example = "KTX")
	String trainClassificationCode,

	@Schema(description = "열차 번호", example = "009")
	String trainNumber,

	@Schema(description = "좌석 선택 가능한 객차 정보 목록")
	List<TrainCarInfo> carInfos
) {
	public static TrainCarListResponse of(Long trainScheduleId, String recommendedCarNumber, int totalCarCount,
		String trainClassificationCode, String trainNumber,
		List<TrainCarInfo> carInfos) {
		return new TrainCarListResponse(trainScheduleId, recommendedCarNumber, totalCarCount,
			trainClassificationCode, trainNumber, carInfos);
	}
}
