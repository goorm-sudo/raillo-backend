package com.sudo.railo.train.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatType {
	WINDOW("창가"),
	AISLE("통로"),
	MIDDLE("중앙");

	private final String description;
}
