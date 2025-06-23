package com.sudo.railo.train.domain.type;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainType {
	KTX("KTX"),
	KTX_SANCHEON("KTX-산천"),
	KTX_CHEONGRYONG("KTX-청룡"),
	KTX_EUM("KTX-이음");

	private final String description;

	public static TrainType fromName(String name) {
		return Arrays.stream(TrainType.values())
			.filter(type -> type.description.equals(name))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("지원하지 않는 열차 유형입니다: " + name));
	}
}
