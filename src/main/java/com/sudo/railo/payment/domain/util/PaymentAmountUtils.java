package com.sudo.railo.payment.domain.util;

import com.sudo.railo.payment.domain.constant.PaymentPrecision;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 결제 금액 계산 유틸리티
 * 
 * BigDecimal 연산 시 정밀도와 반올림 규칙을 통일하여 적용
 * 모든 금액 계산은 이 유틸리티를 통해 수행
 */
public final class PaymentAmountUtils {
    
    /**
     * 기본 반올림 모드 - 반올림(HALF_UP)
     */
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
    
    /**
     * 마일리지 반올림 모드 - 버림(DOWN)
     */
    private static final RoundingMode MILEAGE_ROUNDING = RoundingMode.DOWN;
    
    private PaymentAmountUtils() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }
    
    /**
     * 금액 정규화 - 원화
     * 
     * @param amount 원본 금액
     * @return 정밀도가 조정된 금액
     */
    public static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(PaymentPrecision.AMOUNT_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * 마일리지 정규화
     * 
     * @param mileage 원본 마일리지
     * @return 정밀도가 조정된 마일리지 (소수점 버림)
     */
    public static BigDecimal normalizeMileage(BigDecimal mileage) {
        if (mileage == null) {
            return BigDecimal.ZERO;
        }
        return mileage.setScale(PaymentPrecision.MILEAGE_SCALE, MILEAGE_ROUNDING);
    }
    
    /**
     * 비율 정규화
     * 
     * @param rate 원본 비율
     * @return 정밀도가 조정된 비율
     */
    public static BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        return rate.setScale(PaymentPrecision.RATE_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * 할인 금액 계산
     * 
     * @param originalAmount 원금액
     * @param discountRate 할인율 (0.1 = 10%)
     * @return 할인 금액
     */
    public static BigDecimal calculateDiscountAmount(BigDecimal originalAmount, BigDecimal discountRate) {
        if (originalAmount == null || discountRate == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = originalAmount.multiply(discountRate);
        return normalizeAmount(discount);
    }
    
    /**
     * 마일리지 적립 금액 계산
     * 
     * @param paidAmount 실제 결제 금액
     * @param earningRate 적립률 (0.01 = 1%)
     * @return 적립 마일리지
     */
    public static BigDecimal calculateMileageEarning(BigDecimal paidAmount, BigDecimal earningRate) {
        if (paidAmount == null || earningRate == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal earning = paidAmount.multiply(earningRate);
        return normalizeMileage(earning); // 마일리지는 소수점 버림
    }
    
    /**
     * 최종 결제 금액 계산
     * 
     * @param originalAmount 원금액
     * @param discountAmount 할인 금액
     * @param mileageDeduction 마일리지 차감 금액
     * @return 최종 결제 금액
     */
    public static BigDecimal calculateFinalAmount(BigDecimal originalAmount, 
                                                 BigDecimal discountAmount, 
                                                 BigDecimal mileageDeduction) {
        BigDecimal original = normalizeAmount(originalAmount);
        BigDecimal discount = normalizeAmount(discountAmount);
        BigDecimal mileage = normalizeAmount(mileageDeduction);
        
        BigDecimal finalAmount = original.subtract(discount).subtract(mileage);
        
        // 음수 방지
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        
        return finalAmount;
    }
    
    /**
     * 금액 유효성 검증
     * 
     * @param amount 검증할 금액
     * @return 유효한 금액인지 여부
     */
    public static boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * 양수 금액 검증
     * 
     * @param amount 검증할 금액
     * @return 양수인지 여부
     */
    public static boolean isPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}