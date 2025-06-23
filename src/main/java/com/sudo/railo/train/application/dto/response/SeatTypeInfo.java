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

	@Schema(description = "화면 표시용 텍스트", example = "일반실")
	String displayText
) {
	public static SeatTypeInfo create(int availableSeats, int totalSeats, int fare,
		int passengerCount, String seatTypeName) {

		SeatAvailabilityStatus status = determineSeatStatus(availableSeats, passengerCount);
		String displayText = createDisplayText(status, seatTypeName, availableSeats, passengerCount);

		return new SeatTypeInfo(availableSeats, totalSeats, fare, status, displayText);
	}

	/**
	 * 좌석 수와 승객 수로 예약 가능한 좌석 상태 결정
	 */
	private static SeatAvailabilityStatus determineSeatStatus(int availableSeats, int passengerCount) {
		if (availableSeats == 0) {
			return SeatAvailabilityStatus.SOLD_OUT;
		} else if (availableSeats < passengerCount) {
			return SeatAvailabilityStatus.LIMITED; // 승객 수보다 적으면 좌석부족
		} else if (availableSeats <= 5) {
			return SeatAvailabilityStatus.FEW_REMAINING;
		} else if (availableSeats <= 10) {
			return SeatAvailabilityStatus.LIMITED; // 6~10석은 좌석부족
		} else {
			return SeatAvailabilityStatus.AVAILABLE;
		}
	}

	/**
	 * KTX 화면에 표시되는 텍스트 생성
	 */
	private static String createDisplayText(SeatAvailabilityStatus status, String seatTypeName,
		int availableSeats, int passengerCount) {
		return switch (status) {
			case SOLD_OUT -> "매진";
			case LIMITED -> {
				// 승객 수보다 적으면 "좌석부족", 그냥 적으면 좌석 수 + 좌석부족
				if (availableSeats < passengerCount) {
					yield "좌석부족";
				} else {
					yield String.format("%s(좌석부족)", seatTypeName);
				}
			}
			case FEW_REMAINING -> String.format("%s(매진임박)", seatTypeName);
			case AVAILABLE -> seatTypeName; // "일반실" or "특실"
			default -> seatTypeName;
		};
	}
}
