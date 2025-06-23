package com.sudo.railo.train.domain.type;

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
}
