package com.sudo.railo.train.application.dto.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sudo.railo.train.domain.type.SeatDirection;
import com.sudo.railo.train.domain.type.SeatType;

import lombok.Getter;

/**
 * 좌석 정보 조회용 Projection
 */
@Getter
public class SeatProjection {

	private final Long seatId;
	private final String seatNumber;
	private final int seatRow;
	private final String seatColumn;
	private final SeatType seatType;
	private final String directionCode;
	private final boolean isReserved;
	private final String specialMessage;

	@QueryProjection
	public SeatProjection(
		Long seatId,
		String seatNumber,
		int seatRow,
		String seatColumn,
		SeatType seatType,
		String directionCode,
		boolean isReserved,
		String specialMessage
	) {
		this.seatId = seatId;
		this.seatNumber = seatNumber;
		this.seatRow = seatRow;
		this.seatColumn = seatColumn;
		this.seatType = seatType;
		this.directionCode = directionCode;
		this.isReserved = isReserved;
		this.specialMessage = specialMessage;
	}

	/**
	 * 좌석 위치 반환 (예: "1D")
	 */
	public String getSeatPosition() {
		return seatRow + seatColumn;
	}

	/**
	 * 예약 가능 여부 반환
	 */
	public boolean isAvailable() {
		return !isReserved;
	}

	/**
	 * 좌석 방향 반환
	 */
	public SeatDirection getSeatDirection() {
		return SeatDirection.fromCode(directionCode);
	}
}
