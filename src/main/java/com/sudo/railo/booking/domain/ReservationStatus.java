package com.sudo.railo.booking.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 예약 상태 enum
 */
@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
	SELECTING("선택중", "좌석을 선택하고 있는 상태"),
	RESERVED("예약완료", "예약이 완료된 상태"),
	PAID("결제완료", "결제가 완료된 상태"),
	CANCELLED("취소", "예약이 취소된 상태"),
	REFUNDED("환불완료", "환불이 완료된 상태"),
	EXPIRED("만료", "예약이 만료된 상태");

	private final String displayName;
	private final String description;

	/**
	 * 활성 상태 여부 (실제 좌석을 점유하는 상태)
	 */
	public boolean isActive() {
		return this == RESERVED || this == PAID;
	}

	/**
	 * 취소 가능 여부
	 */
	public boolean isCancellable() {
		return this == RESERVED || this == PAID;
	}

	/**
	 * 결제 가능 여부
	 */
	public boolean isPayable() {
		return this == RESERVED;
	}

	/**
	 * 환불 가능 여부
	 */
	public boolean isRefundable() {
		return this == PAID;
	}
}
