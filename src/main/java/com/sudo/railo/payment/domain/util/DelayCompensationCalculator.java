package com.sudo.railo.payment.domain.util;

import java.math.BigDecimal;

/**
 * 지연 보상 비율 계산 유틸리티
 * 열차 지연 시간에 따른 마일리지 보상 비율을 계산
 */
public final class DelayCompensationCalculator {
    
    // 지연 시간 기준 (분)
    private static final int DELAY_THRESHOLD_20_MINUTES = 20;
    private static final int DELAY_THRESHOLD_40_MINUTES = 40;
    private static final int DELAY_THRESHOLD_60_MINUTES = 60;
    
    // 보상 비율
    private static final BigDecimal COMPENSATION_RATE_12_5_PERCENT = new BigDecimal("0.125");
    private static final BigDecimal COMPENSATION_RATE_25_PERCENT = new BigDecimal("0.25");
    private static final BigDecimal COMPENSATION_RATE_50_PERCENT = new BigDecimal("0.50");
    
    private DelayCompensationCalculator() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
    
    /**
     * 지연 시간에 따른 보상 비율 계산 (BigDecimal 반환)
     * 
     * @param delayMinutes 지연 시간(분)
     * @return 보상 비율 (0.0 ~ 0.5)
     */
    public static BigDecimal calculateCompensationRate(int delayMinutes) {
        if (delayMinutes >= DELAY_THRESHOLD_60_MINUTES) {
            return COMPENSATION_RATE_50_PERCENT;    // 50% 보상
        } else if (delayMinutes >= DELAY_THRESHOLD_40_MINUTES) {
            return COMPENSATION_RATE_25_PERCENT;    // 25% 보상
        } else if (delayMinutes >= DELAY_THRESHOLD_20_MINUTES) {
            return COMPENSATION_RATE_12_5_PERCENT;  // 12.5% 보상
        } else {
            return BigDecimal.ZERO;                 // 보상 없음
        }
    }
    
    /**
     * 지연 시간에 따른 보상 비율 계산 (double 반환)
     * Train 도메인 호환용
     * 
     * @param delayMinutes 지연 시간(분)
     * @return 보상 비율 (0.0 ~ 0.5)
     */
    public static double calculateCompensationRateAsDouble(int delayMinutes) {
        return calculateCompensationRate(delayMinutes).doubleValue();
    }
    
    /**
     * 지연 보상 대상 여부 확인
     * 
     * @param delayMinutes 지연 시간(분)
     * @return 20분 이상 지연 시 true
     */
    public static boolean isEligibleForCompensation(int delayMinutes) {
        return delayMinutes >= DELAY_THRESHOLD_20_MINUTES;
    }
    
    /**
     * 지연 보상 금액 계산
     * 
     * @param originalAmount 원래 결제 금액
     * @param delayMinutes 지연 시간(분)
     * @return 보상 금액 (소수점 이하 버림)
     */
    public static BigDecimal calculateCompensationAmount(BigDecimal originalAmount, int delayMinutes) {
        BigDecimal compensationRate = calculateCompensationRate(delayMinutes);
        
        if (compensationRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return originalAmount
                .multiply(compensationRate)
                .setScale(0, BigDecimal.ROUND_DOWN);
    }
    
    /**
     * 지연 단계별 설명 반환
     * 
     * @param delayMinutes 지연 시간(분)
     * @return 지연 단계 설명
     */
    public static String getDelayCompensationDescription(int delayMinutes) {
        if (delayMinutes >= DELAY_THRESHOLD_60_MINUTES) {
            return "60분 이상 지연 (50% 보상)";
        } else if (delayMinutes >= DELAY_THRESHOLD_40_MINUTES) {
            return "40분 이상 지연 (25% 보상)";
        } else if (delayMinutes >= DELAY_THRESHOLD_20_MINUTES) {
            return "20분 이상 지연 (12.5% 보상)";
        } else {
            return "지연 보상 없음";
        }
    }
} 