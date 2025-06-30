package com.sudo.railo.train.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 구간별 좌석 상태 종합 정보
 * Service Layer에서 좌석 예약 가능 여부를 판단
 *
 * - 일반실/특실별 잔여 좌석 수
 * - 요청한 승객 수로 예약 가능 여부
 * - 입석 가능 여부 및 수량
 * - 구간 내 최대 탑승 인원 (입석 판단용)
 */
@Schema(description = "구간별 좌석 상태 종합 정보")
public record SectionSeatStatus(
	// 일반실 정보
	@Schema(description = "일반실 잔여 좌석 수", example = "45")
	int standardAvailable,

	@Schema(description = "일반실 전체 좌석 수", example = "246")
	int standardTotal,

	// 특실 정보
	@Schema(description = "특실 잔여 좌석 수", example = "12")
	int firstClassAvailable,

	@Schema(description = "특실 전체 좌석 수", example = "117")
	int firstClassTotal,

	// 예약 가능 여부
	@Schema(description = "일반실 예약 가능 여부", example = "true")
	boolean canReserveStandard,

	@Schema(description = "특실 예약 가능 여부", example = "true")
	boolean canReserveFirstClass,

	// 입석 정보
	@Schema(description = "입석 가능 여부", example = "true")
	boolean standingAvailable,

	@Schema(description = "추가 입석 가능 최대 인원", example = "25")
	int maxAdditionalStanding,

	@Schema(description = "입석 예약 가능 여부", example = "true")
	boolean canReserveStanding,

	// 구간 분석 정보
	@Schema(description = "해당 구간 내 최대 탑승 인원", example = "340")
	int maxOccupancyInRoute
) {
	/**
	 * 정적 팩토리 메서드
	 */
	public static SectionSeatStatus of(
		int standardAvailable, int standardTotal,
		int firstClassAvailable, int firstClassTotal,
		boolean canReserveStandard, boolean canReserveFirstClass,
		boolean standingAvailable, int maxAdditionalStanding,
		boolean canReserveStanding, int maxOccupancyInRoute) {

		return new SectionSeatStatus(
			standardAvailable, standardTotal, firstClassAvailable, firstClassTotal,
			canReserveStandard, canReserveFirstClass,
			standingAvailable, maxAdditionalStanding, canReserveStanding, maxOccupancyInRoute
		);
	}
}
