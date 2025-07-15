package com.sudo.railo.payment.application.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 저장된 결제수단 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedPaymentMethodResponseDto {
    
    private Long id;
    private Long memberId;
    private String paymentMethodType;
    private String alias;
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    
    // 마스킹된 정보 (항상 반환)
    private String maskedCardNumber;
    private String maskedAccountNumber;
    private String bankCode;
    
    // 실제 정보 (특별한 권한 필요)
    private String cardNumber;
    private String cardHolderName;
    private String cardExpiryMonth;
    private String cardExpiryYear;
    private String accountNumber;
    private String accountHolderName;
}