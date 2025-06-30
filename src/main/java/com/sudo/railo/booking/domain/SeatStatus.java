package com.sudo.railo.booking.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatStatus {
	AVAILABLE("예약가능"),
	RESERVED("예매 완료"),
	LOCKED("잠금 상태");

	private final String description;
}
