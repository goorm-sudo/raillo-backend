package com.sudo.railo.train.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatAvailabilityStatus {

	AVAILABLE("예약가능"),        // 11석 이상
	LIMITED("좌석부족"),          // 6~10석
	FEW_REMAINING("매진임박"),    // 1~5석
	SOLD_OUT("매진");            // 0석 (매진)

	private final String description;
}
