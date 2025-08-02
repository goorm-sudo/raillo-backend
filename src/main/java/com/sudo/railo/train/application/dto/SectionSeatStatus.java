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
	int standardRemaining,

	@Schema(description = "일반실 전체 좌석 수", example = "246")
	int standardTotal,

	// 특실 정보
	@Schema(description = "특실 잔여 좌석 수", example = "12")
	int firstClassRemaining,

	@Schema(description = "특실 전체 좌석 수", example = "117")
	int firstClassTotal,

	// 예약 가능 여부
	@Schema(description = "승객수 고려한 일반실 예약 가능 여부", example = "true")
	boolean canReserveStandard,

	@Schema(description = "승객수 고려한 특실 예약 가능 여부", example = "true")
	boolean canReserveFirstClass,

	// 입석 정보
	@Schema(description = "현재 예약된 입석 인원", example = "15")
	int currentStandingReservations,

	@Schema(description = "전체 좌석 수 (입석 계산용)", example = "363")
	int totalSeats

) {
	/**
	 * 정적 팩토리 메서드
	 */
	public static SectionSeatStatus of(
		int standardRemaining, int standardTotal,
		int firstClassRemaining, int firstClassTotal,
		boolean canReserveStandard, boolean canReserveFirstClass,
		int currentStandingReservations, int totalSeats) {

		return new SectionSeatStatus(
			standardRemaining, standardTotal,
			firstClassRemaining, firstClassTotal,
			canReserveStandard, canReserveFirstClass,
			currentStandingReservations, totalSeats
		);
	}

	/**
	 * 열차의 최대 입석 수용 인원
	 * @param standingRatio
	 * @return
	 */
	public int getMaxStandingCapacity(double standingRatio) {
		return (int)(totalSeats * standingRatio);
	}

	/**
	 * 잔여 입석 개수
	 * @param standingRatio
	 * @return
	 */
	public int getStandingRemaining(double standingRatio) {
		return Math.max(0, getMaxStandingCapacity(standingRatio) - currentStandingReservations);
	}

	/**
	 *  승객수로 입석 예약 가능 여부 판단
	 * @param passengerCount
	 * @param standingRatio
	 * @return
	 */
	public boolean canReserveStanding(int passengerCount, double standingRatio) {
		return getStandingRemaining(standingRatio) >= passengerCount;
	}
}
