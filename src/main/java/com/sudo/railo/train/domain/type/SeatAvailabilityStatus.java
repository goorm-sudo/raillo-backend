package com.sudo.railo.train.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatAvailabilityStatus {

	AVAILABLE("여유", "충분한 좌석이 있습니다"),
	LIMITED("매진임박", "좌석이 얼마 남지 않았습니다"),
	INSUFFICIENT("좌석부족", "요청하신 인원보다 좌석이 부족합니다"),
	SOLD_OUT("매진", "모든 좌석이 매진되었습니다"),
	STANDING_ONLY("입석", "좌석은 매진이지만 입석으로 이용 가능합니다");

	private final String text;
	private final String description;
}
