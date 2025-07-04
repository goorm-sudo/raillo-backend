package com.sudo.railo.payment.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {
    
    /**
     * 결제 계산 세션 만료 시간 (분)
     */
    private int calculationExpiryMinutes = 30;
    
    /**
     * 최근 거래 조회 제한 개수
     */
    private int recentTransactionsLimit = 10;
    
    /**
     * 최대 재시도 횟수
     */
    private int maxRetryAttempts = 3;
    
    /**
     * 락 타임아웃 시간
     */
    private Duration lockTimeout = Duration.ofSeconds(10);
    
    /**
     * 마일리지 관련 설정
     */
    private MileageConfig mileage = new MileageConfig();
    
    /**
     * 결제 검증 관련 설정
     */
    private ValidationConfig validation = new ValidationConfig();
    
    /**
     * 배치 처리 관련 설정
     */
    private BatchConfig batch = new BatchConfig();
    
    @Data
    public static class MileageConfig {
        /**
         * 마일리지 전환율 (1원당 적립 마일리지)
         */
        private BigDecimal conversionRate = new BigDecimal("1.0");
        
        /**
         * 최대 마일리지 사용 비율 (전체 결제금액의 100%)
         */
        private BigDecimal maxUsageRate = new BigDecimal("1.0");
        
        /**
         * 마일리지 최소 사용 금액
         */
        private BigDecimal minUsageAmount = new BigDecimal("1000");
        
        /**
         * 마일리지 유효기간 (개월)
         */
        private int validityMonths = 24;
        
        /**
         * 마일리지 적립 최소 결제금액
         */
        private BigDecimal minEarnAmount = new BigDecimal("1000");
    }
    
    @Data
    public static class ValidationConfig {
        /**
         * 최대 결제 금액 제한
         */
        private BigDecimal maxPaymentAmount = new BigDecimal("10000000");
        
        /**
         * 최소 결제 금액
         */
        private BigDecimal minPaymentAmount = new BigDecimal("100");
        
        /**
         * 비회원 최대 결제 금액
         */
        private BigDecimal maxNonMemberPaymentAmount = new BigDecimal("500000");
        
        /**
         * 일일 최대 결제 횟수 (회원)
         */
        private int maxDailyPaymentCount = 50;
        
        /**
         * 일일 최대 결제 횟수 (비회원)
         */
        private int maxDailyNonMemberPaymentCount = 10;
    }
    
    @Data
    public static class BatchConfig {
        /**
         * 배치 처리 크기
         */
        private int batchSize = 1000;
        
        /**
         * 만료 세션 정리 주기 (분)
         */
        private int expiredSessionCleanupIntervalMinutes = 60;
        
        /**
         * 마일리지 만료 처리 주기 (시간)
         */
        private int mileageExpiryProcessIntervalHours = 24;
        
        /**
         * 통계 집계 주기 (시간)
         */
        private int statisticsAggregationIntervalHours = 6;
    }
}
