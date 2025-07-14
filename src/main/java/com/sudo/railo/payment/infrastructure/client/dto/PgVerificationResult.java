package com.sudo.railo.payment.infrastructure.client.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PG 검증 결과 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PgVerificationResult {
    
    private boolean success;
    private BigDecimal amount;
    private String authNumber;
    private LocalDateTime approvedAt;
    private String message;
    private String cardNumber; // 마스킹된 카드번호
    private String cardType;
    
    // 성공 결과 생성
    public static PgVerificationResult success(BigDecimal amount, String authNumber) {
        return PgVerificationResult.builder()
            .success(true)
            .amount(amount)
            .authNumber(authNumber)
            .approvedAt(LocalDateTime.now())
            .build();
    }
    
    // 실패 결과 생성
    public static PgVerificationResult fail(String message) {
        return PgVerificationResult.builder()
            .success(false)
            .message(message)
            .build();
    }
}