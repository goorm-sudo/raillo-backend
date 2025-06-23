package com.sudo.railo.train.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 열차 상세
 */
@Schema(description = "열차 상세 정보")
public record TrainDetailResponse(
	@Schema(description = "기본 열차 정보")
	TrainSearchResponse basicInfo,

	@Schema(description = "정차역 목록")
	List<StationStopInfo> stopStations,

	@Schema(description = "열차 편성 정보")
	TrainCompositionInfo composition,

	@Schema(description = "좌석 배치도 정보")
	List<CarLayoutInfo> seatLayout
) {
	public static TrainDetailResponse of(
		TrainSearchResponse basicInfo,
		List<StationStopInfo> stopStations,
		TrainCompositionInfo composition,
		List<CarLayoutInfo> seatLayout) {
		return new TrainDetailResponse(basicInfo, stopStations, composition, seatLayout);
	}
}
