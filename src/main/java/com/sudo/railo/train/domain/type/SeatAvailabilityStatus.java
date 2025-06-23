package com.sudo.railo.train.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatAvailabilityStatus {

	AVAILABLE("여유"),        // 11석 이상
	LIMITED("좌석부족"),          // 6~10석
	FEW_REMAINING("매진임박"),    // 1~5석
	FIRST_CLASS_ONLY("특실만 남음"),
	STANDING_AVAILABLE("입석+좌석"), // 좌석이 부족하지만 입석 가능
	SOLD_OUT("매진");            // 0석 (매진)

	private final String description;
}
