package com.sudo.railo.payment.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.sudo.railo.payment.domain.Payment;
import com.sudo.railo.payment.domain.status.PaymentStatus;
import com.sudo.railo.payment.domain.type.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 처리 응답")
public record PaymentProcessResponse(

	@Schema(description = "결제 ID", example = "1")
	Long paymentId,

	@Schema(description = "결제 고유 번호 (결제 발생 일자-회원 번호-결제 순번)", example = "20250723-202309210103-001")
	String paymentKey,

	@Schema(description = "결제 금액", example = "50000")
	BigDecimal amount,

	@Schema(description = "결제 수단", example = "CARD")
	PaymentMethod paymentMethod,

	@Schema(description = "결제 상태", example = "PAID")
	PaymentStatus paymentStatus,

	@Schema(description = "결제 완료 시간")
	LocalDateTime paidAt
) {

	public static PaymentProcessResponse from(Payment payment) {
		return new PaymentProcessResponse(payment.getId(), payment.getPaymentKey(), payment.getAmount(),
			payment.getPaymentMethod(), payment.getPaymentStatus(), payment.getPaidAt());
	}
}
