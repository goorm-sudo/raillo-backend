package com.sudo.railo.payment.application.dto.request;

import java.math.BigDecimal;

import com.sudo.railo.payment.domain.type.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public abstract class PaymentProcessRequest {

    @Schema(description = "예약 ID", example = "1")
    @NotNull(message = "예약 ID는 필수입니다")
    private Long reservationId;

    @Schema(description = "결제 금액", example = "50000")
    @NotNull(message = "결제 금액은 필수입니다")
    @Positive(message = "결제 금액은 0보다 커야 합니다")
    private BigDecimal amount;

    /**
     * 하위 클래스에서 PaymentMethod를 반환하도록 구현
     */
    public abstract PaymentMethod getPaymentMethod();
}
