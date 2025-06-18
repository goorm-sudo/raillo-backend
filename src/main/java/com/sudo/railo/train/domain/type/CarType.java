package com.sudo.railo.train.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CarType {
	STANDARD("일반실"),
	FIRST_CLASS("특실");

	private final String description;
}
