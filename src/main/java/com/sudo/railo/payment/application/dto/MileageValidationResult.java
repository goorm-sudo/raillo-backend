package com.sudo.railo.payment.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 마일리지 검증 결과
 * 
 * 마일리지 사용 가능 여부와 차감 금액 정보를 담은 DTO
 */
@Getter
@Builder
public class MileageValidationResult {
    
    /**
     * 검증 성공 여부
     */
    private final boolean valid;
    
    /**
     * 사용할 마일리지 포인트
     */
    private final BigDecimal usageAmount;
    
    /**
     * 사용 가능한 마일리지 잔액
     */
    private final BigDecimal availableBalance;
    
    /**
     * 마일리지로 차감될 원화 금액
     */
    private final BigDecimal deductionAmount;
    
    /**
     * 검증 실패 사유 (실패 시에만)
     */
    private final String failureReason;
    
    /**
     * 마일리지 사용 여부 확인
     */
    public boolean hasMileageUsage() {
        return usageAmount != null && usageAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * 성공 결과 생성 팩토리 메서드
     */
    public static MileageValidationResult success(BigDecimal usageAmount, 
                                                 BigDecimal availableBalance,
                                                 BigDecimal deductionAmount) {
        return MileageValidationResult.builder()
                .valid(true)
                .usageAmount(usageAmount)
                .availableBalance(availableBalance)
                .deductionAmount(deductionAmount)
                .build();
    }
    
    /**
     * 실패 결과 생성 팩토리 메서드
     */
    public static MileageValidationResult failure(String reason) {
        return MileageValidationResult.builder()
                .valid(false)
                .usageAmount(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .deductionAmount(BigDecimal.ZERO)
                .failureReason(reason)
                .build();
    }
    
    /**
     * 마일리지 미사용 결과 생성 팩토리 메서드
     */
    public static MileageValidationResult notUsed() {
        return MileageValidationResult.builder()
                .valid(true)
                .usageAmount(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .deductionAmount(BigDecimal.ZERO)
                .build();
    }
}