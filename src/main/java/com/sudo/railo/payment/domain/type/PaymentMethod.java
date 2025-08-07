package com.sudo.railo.payment.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
	CARD("카드", "카드 결제"),
	TRANSFER("계좌이체", "계좌이체 결제");

	private final String displayName;
	private final String description;
}
