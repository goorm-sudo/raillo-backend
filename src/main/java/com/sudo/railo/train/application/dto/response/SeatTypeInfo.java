package com.sudo.railo.train.application.dto.response;

import com.sudo.railo.train.domain.type.SeatAvailabilityStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "좌석 유형별 정보")
public record SeatTypeInfo(
	@Schema(description = "잔여 좌석 수", example = "45")
	int availableSeats,

	@Schema(description = "전체 좌석 수", example = "300")
	int totalSeats,

	@Schema(description = "요금", example = "14100")
	int fare,

	@Schema(description = "좌석 상태", example = "AVAILABLE")
	SeatAvailabilityStatus status,

	@Schema(description = "예약 가능 여부", example = "true")
	boolean canReserve,

	@Schema(description = "화면 표시용 텍스트", example = "일반실")
	String displayText
) {
	// 입석 정보 포함
	public static SeatTypeInfo create(int availableSeats, int totalSeats, int fare,
		int passengerCount, String seatTypeName, boolean hasStanding) {

		SeatAvailabilityStatus status = determineSeatStatus(availableSeats, passengerCount, hasStanding);
		boolean canReserve = availableSeats >= passengerCount;
		String displayText = createDisplayText(status, seatTypeName, availableSeats, passengerCount);

		return new SeatTypeInfo(availableSeats, totalSeats, fare, status, canReserve, displayText);
	}

	/**
	 * 좌석 수와 승객 수로 예약 가능한 좌석 상태 결정
	 * // TODO : 기준 값 변수로 처리
	 */
	private static SeatAvailabilityStatus determineSeatStatus(int availableSeats, int passengerCount,
		boolean hasStanding) {
		if (availableSeats >= 11) {
			return SeatAvailabilityStatus.AVAILABLE;
		} else if (availableSeats >= 6) {
			return SeatAvailabilityStatus.LIMITED;
		} else if (availableSeats >= 1) {
			return SeatAvailabilityStatus.FEW_REMAINING;
		} else if (availableSeats == 0 && hasStanding) {
			return SeatAvailabilityStatus.STANDING_AVAILABLE;  // 좌석 없지만 입석 가능
		} else {
			return SeatAvailabilityStatus.SOLD_OUT;
		}
	}

	/**
	 * 화면 표시용 텍스트 생성
	 */
	private static String createDisplayText(SeatAvailabilityStatus status, String seatTypeName,
		int availableSeats, int passengerCount) {
		return switch (status) {
			case SOLD_OUT -> "매진";
			case FEW_REMAINING -> String.format("%s(매진임박)", seatTypeName);
			case STANDING_AVAILABLE -> "입석+좌석";
			case LIMITED -> {
				// 승객 수보다 적으면 "좌석부족", 충분하면 "좌석유형(좌석부족)"
				if (availableSeats < passengerCount) {
					yield "좌석부족";
				} else {
					yield String.format("%s(좌석부족)", seatTypeName);
				}
			}
			case AVAILABLE -> seatTypeName; // "일반실" or "특실"
		};
	}
}
