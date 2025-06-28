package com.sudo.railo.train.application.dto.excel;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StationFareHeaderType {
	SECTION("구간"),
	STANDARD("일반실"),
	SUPERIOR_CLASS("우등실"),
	FIRST_CLASS("특실");

	private final String description;

	public static StationFareHeaderType from(String value) {
		return Arrays.stream(StationFareHeaderType.values())
			.filter(type -> value.contains(type.description))
			.findFirst()
			.orElse(null);
	}
}
