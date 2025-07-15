package com.sudo.railo.payment.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedPaymentMethodRequestDto {

    @NotNull(message = "회원 ID는 필수입니다.")
    private Long memberId;

    @NotBlank(message = "결제수단 타입은 필수입니다.")
    private String paymentMethodType; // CREDIT_CARD, BANK_ACCOUNT

    @NotBlank(message = "별명은 필수입니다.")
    private String alias; // "표시 명" → "별명"으로 변경

    // 신용카드 관련 필드
    private String cardNumber;
    private String cardHolderName;
    private String cardExpiryMonth;
    private String cardExpiryYear;
    private String cardCvc; // CVC 필드 추가

    // 계좌 관련 필드
    private String bankCode;
    private String accountNumber;
    private String accountHolderName;
    private String accountPassword; // 계좌 비밀번호 필드 추가

    @Builder.Default
    private Boolean isDefault = false;
} 