package com.sudo.railo.payment.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedPaymentMethodResponseDto {

    private Long id;
    private Long memberId;
    private String paymentMethodType;
    private String alias;

    // 신용카드 관련 필드 (마스킹된 정보)
    private String cardNumber;
    private String cardHolderName;
    private String cardExpiryMonth;
    private String cardExpiryYear;
    // CVC는 보안상 응답에 포함하지 않음

    // 계좌 관련 필드
    private String bankCode;
    private String accountNumber;
    private String accountHolderName;
    // 계좌 비밀번호는 보안상 응답에 포함하지 않음

    private Boolean isDefault;
    private LocalDateTime createdAt;
} 