package com.sudo.railo.payment.application.dto.request;

import java.math.BigDecimal;

import com.sudo.railo.payment.domain.type.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "결제 처리 요청")
public record PaymentProcessRequest(

	@Schema(description = "예약 ID", example = "1")
	@NotNull(message = "예약 ID는 필수입니다")
	Long reservationId,

	@Schema(description = "결제 금액", example = "50000")
	@NotNull(message = "결제 금액은 필수입니다")
	@Positive(message = "결제 금액은 0보다 커야 합니다")
	BigDecimal amount,

	@Schema(description = "결제 수단", example = "CARD")
	@NotNull(message = "결제 수단은 필수입니다")
	PaymentMethod paymentMethod
) {
}
