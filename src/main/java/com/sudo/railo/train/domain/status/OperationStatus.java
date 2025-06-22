package com.sudo.railo.train.domain.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OperationStatus {
	ACTIVE("정상운행"),          // 정상 운행 중
	DELAYED("지연"),            // 5분 이상 지연
	SUSPENDED("운행중단"),       // 일시적 운행 중단
	CANCELLED("운행취소");       // 완전 취소

	private final String description;
}
