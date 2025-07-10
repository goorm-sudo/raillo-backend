package com.sudo.railo.booking.application.dto.request;

import java.math.BigDecimal;

import com.sudo.railo.booking.domain.PassengerType;

import jakarta.validation.constraints.NotNull;

public record FareCalculateRequest(
	@NotNull(message = "승객 유형은 필수입니다")
	PassengerType passengerType,

	@NotNull(message = "원래 운임은 필수입니다")
	BigDecimal fare
) {
}
