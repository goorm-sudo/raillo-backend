package com.sudo.railo.payment.application.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 마일리지 잔액 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class MileageBalanceInfo {
    
    private Long memberId;
    private BigDecimal currentBalance;           // 현재 총 잔액
    private BigDecimal activeBalance;            // 활성 잔액 (만료되지 않은 것만)
    private BigDecimal expiringMileage;         // 30일 이내 만료 예정 마일리지
    private LocalDateTime lastTransactionAt;     // 마지막 거래 시간
    private MileageStatistics statistics;        // 통계 정보
    private List<TransactionSummary> recentTransactions; // 최근 거래 내역 요약
    
    /**
     * 거래 요약 정보
     */
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class TransactionSummary {
        
        private Long transactionId;
        private String type;                    // 거래 유형
        private BigDecimal amount;              // 거래 금액
        private String description;             // 설명
        private LocalDateTime processedAt;      // 처리 시간
        private String status;                  // 상태
    }
} 