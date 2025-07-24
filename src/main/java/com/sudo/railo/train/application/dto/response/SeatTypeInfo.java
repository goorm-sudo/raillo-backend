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
	String displayText,

	@Schema(description = "상세 설명", example = "잔여 45석")
	String description
) {
	// 입석 정보 포함
	public static SeatTypeInfo create(int availableSeats,
		int totalSeats,
		int fare,
		int passengerCount,
		String seatTypeName) {

		SeatAvailabilityStatus status = determineSeatStatus(availableSeats, passengerCount);
		boolean canReserve = availableSeats >= passengerCount;

		String displayText = createDisplayText(status, seatTypeName);
		String description = createDescription(status, availableSeats, passengerCount);

		return new SeatTypeInfo(availableSeats, totalSeats, fare, status, canReserve, displayText, description);
	}

	/**
	 * 좌석 수와 승객 수로 예약 가능한 좌석 상태 결정
	 * // TODO : 기준 값 변수로 처리
	 */
	private static SeatAvailabilityStatus determineSeatStatus(int availableSeats, int passengerCount) {
		if (availableSeats == 0) {
			return SeatAvailabilityStatus.SOLD_OUT;  // 단순하게 매진으로만 처리
		}

		if (availableSeats < passengerCount) {
			return SeatAvailabilityStatus.INSUFFICIENT;
		}

		if (availableSeats >= passengerCount + 20) {
			return SeatAvailabilityStatus.AVAILABLE;
		} else {
			return SeatAvailabilityStatus.LIMITED;
		}
	}

	/**
	 * 화면 표시용 텍스트 생성
	 */
	private static String createDisplayText(SeatAvailabilityStatus status, String seatTypeName) {
		return switch (status) {
			case SOLD_OUT -> status.getText();           // "매진"
			case INSUFFICIENT -> status.getText();       // "좌석부족"
			case LIMITED -> seatTypeName + "(" + status.getText() + ")";  // "일반실(매진임박)"
			case AVAILABLE -> seatTypeName;              // "일반실" / "특실"
			// STANDING_ONLY 케이스 제거
		};
	}

	/**
	 * 상세 설명 생성 (enum description + 구체적 수치)
	 */
	private static String createDescription(SeatAvailabilityStatus status, int availableSeats,
		int passengerCount) {
		return switch (status) {
			case SOLD_OUT -> status.getDescription();
			case INSUFFICIENT -> String.format("%s (%d명 예약 시 %d석 부족)",
				status.getDescription(), passengerCount, passengerCount - availableSeats);
			case LIMITED -> String.format("%s (잔여 %d석)", status.getDescription(), availableSeats);
			case AVAILABLE -> String.format("잔여 %d석", availableSeats);
		};
	}
}
