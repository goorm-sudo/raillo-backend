package com.sudo.railo.train.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 좌석 방향
 */
@Getter
@RequiredArgsConstructor
public enum SeatDirection {

	FORWARD("009", "순방향"),
	BACKWARD("010", "역방향");

	private final String code;
	private final String description;

	public static SeatDirection fromCode(String code) {
		for (SeatDirection direction : values()) {
			if (direction.code.equals(code)) {
				return direction;
			}
		}
		return FORWARD; // 기본값
	}
}
