package com.sudo.railo.payment.application.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 마일리지 통계 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class MileageStatistics {
    
    private Integer totalTransactions;          // 총 거래 건수
    private Integer earnTransactionCount;       // 적립 거래 건수
    private Integer useTransactionCount;        // 사용 거래 건수
    
    private BigDecimal totalEarned;             // 총 적립 포인트
    private BigDecimal totalUsed;               // 총 사용 포인트
    private BigDecimal netAmount;               // 순 증감 (적립 - 사용)
    
    private BigDecimal averageEarningPerTransaction;  // 거래당 평균 적립
    private BigDecimal averageUsagePerTransaction;    // 거래당 평균 사용
    
    private LocalDateTime firstTransactionAt;   // 첫 거래 시간
    private LocalDateTime lastEarningAt;        // 마지막 적립 시간
    private LocalDateTime lastUsageAt;          // 마지막 사용 시간
    
    /**
     * 순 증감 계산 (적립 - 사용)
     */
    public BigDecimal getNetAmount() {
        if (totalEarned == null) totalEarned = BigDecimal.ZERO;
        if (totalUsed == null) totalUsed = BigDecimal.ZERO;
        return totalEarned.subtract(totalUsed);
    }
    
    /**
     * 적립 비율 계산 (총 적립 / 총 거래)
     */
    public String getEarningRate() {
        if (totalTransactions == null || totalTransactions == 0) {
            return "0%";
        }
        if (earnTransactionCount == null) {
            return "0%";
        }
        double rate = (double) earnTransactionCount / totalTransactions * 100;
        return String.format("%.1f%%", rate);
    }
    
    /**
     * 사용 비율 계산 (총 사용 / 총 거래)
     */
    public String getUsageRate() {
        if (totalTransactions == null || totalTransactions == 0) {
            return "0%";
        }
        if (useTransactionCount == null) {
            return "0%";
        }
        double rate = (double) useTransactionCount / totalTransactions * 100;
        return String.format("%.1f%%", rate);
    }
} 