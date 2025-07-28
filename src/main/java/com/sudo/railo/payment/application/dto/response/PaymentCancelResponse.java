package com.sudo.railo.payment.application.dto.response;

import java.time.LocalDateTime;

import com.sudo.railo.payment.domain.Payment;
import com.sudo.railo.payment.domain.status.PaymentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 취소 응답")
public record PaymentCancelResponse(

	@Schema(description = "결제 ID", example = "1")
	Long paymentId,

	@Schema(description = "결제 고유 번호 (결제 발생 일자-회원 번호-결제 순번)", example = "20250723-202309210103-001")
	String paymentKey,

	@Schema(description = "결제 상태", example = "CANCELLED")
	PaymentStatus paymentStatus,

	@Schema(description = "결제 완료 시간")
	LocalDateTime cancelledAt
) {

	public static PaymentCancelResponse from(Payment payment) {
		return new PaymentCancelResponse(payment.getId(), payment.getPaymentKey(), payment.getPaymentStatus(),
			payment.getCancelledAt());
	}
}
