package com.sudo.railo.payment.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.sudo.railo.payment.domain.status.PaymentStatus;
import com.sudo.railo.payment.domain.type.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 내역 응답")
public record PaymentHistoryResponse(

	@Schema(description = "결제 ID", example = "1")
	Long paymentId,

	@Schema(description = "결제 키", example = "PAY_1234567890ABCDEF")
	String paymentKey,

	@Schema(description = "예약 코드", example = "202312251230A1B2")
	String reservationCode,

	@Schema(description = "결제 금액", example = "50000")
	BigDecimal amount,

	@Schema(description = "결제 수단", example = "CARD")
	PaymentMethod paymentMethod,

	@Schema(description = "결제 상태", example = "PAID")
	PaymentStatus paymentStatus,

	@Schema(description = "결제 완료 시간")
	LocalDateTime paidAt,

	@Schema(description = "결제 취소 시간")
	LocalDateTime cancelledAt,

	@Schema(description = "환불 완료 시간")
	LocalDateTime refundedAt
) {
}
