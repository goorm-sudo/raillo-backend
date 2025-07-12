package com.sudo.railo.payment.application.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;

/**
 * 결제수단 저장 요청 DTO (프론트엔드용)
 * memberId는 JWT 토큰에서 자동으로 추출되므로 포함하지 않음
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSavedPaymentMethodRequest {

    @NotBlank(message = "결제수단 타입은 필수입니다.")
    private String paymentMethodType; // CREDIT_CARD, BANK_ACCOUNT

    @NotBlank(message = "별명은 필수입니다.")
    private String alias;

    // 신용카드 관련 필드
    private String cardNumber;
    private String cardHolderName;
    private String cardExpiryMonth;
    private String cardExpiryYear;
    private String cardCvc;

    // 계좌 관련 필드
    private String bankCode;
    private String accountNumber;
    private String accountHolderName;
    private String accountPassword;

    @Builder.Default
    private Boolean isDefault = false;
}