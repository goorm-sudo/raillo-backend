package com.sudo.railo.train.application.dto.response;

import com.sudo.railo.train.domain.type.SeatDirection;
import com.sudo.railo.train.domain.type.SeatType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 좌석 상세 정보
 */
public record SeatDetail(

	@Schema(description = "좌석 ID", example = "1")
	Long seatId,

	@Schema(description = "좌석 번호 (행 + 열)", example = "1D")
	String seatNumber,

	@Schema(description = "예약 가능 여부", example = "true")
	boolean isAvailable,

	@Schema(description = "좌석 방향", example = "FORWARD")
	SeatDirection seatDirection,

	@Schema(description = "좌석 타입", example = "WINDOW")
	SeatType seatType,

	@Schema(description = "비고 메시지(4인 동반석 등)", example = "KTX 4인동반석 역방향 좌석 입니다. 맞은편 좌석에 다른 승객이 승차할 수 있습니다.")
	String remarks
) {
	public static SeatDetail of(
		Long seatId,
		String seatNumber,
		boolean isAvailable,
		SeatDirection seatDirection,
		SeatType seatType,
		String specialMessage
	) {
		return new SeatDetail(
			seatId,
			seatNumber,
			isAvailable,
			seatDirection,
			seatType,
			specialMessage
		);
	}
}
