package com.sudo.railo.payment.application.dto.response;

import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 상세 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentInfoResponse {
    
    // 기본 결제 정보
    private Long paymentId;
    private Long reservationId;
    private String externalOrderId;
    
    // 금액 정보
    private BigDecimal amountOriginalTotal;
    private BigDecimal totalDiscountAmountApplied;
    private BigDecimal mileagePointsUsed;
    private BigDecimal mileageAmountDeducted;
    private BigDecimal mileageToEarn;
    private BigDecimal amountPaid;
    
    // 결제 상태 및 방법
    private PaymentExecutionStatus paymentStatus;
    private String paymentMethod;
    private String pgProvider;
    private String pgTransactionId;
    private String pgApprovalNo;
    private String receiptUrl;
    
    // 시간 정보
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    
    // 비회원 정보 (마스킹 처리)
    private String nonMemberName;
    private String nonMemberPhoneMasked;
    
    // 마일리지 거래 내역
    private List<MileageTransactionInfo> mileageTransactions;
    
    // 할인 적용 내역
    private List<DiscountInfo> discountDetails;
    
    /**
     * 마일리지 거래 내역 정보
     */
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class MileageTransactionInfo {
        
        private Long transactionId;
        private String transactionType;     // 거래 유형 (적립, 사용, 만료 등)
        private BigDecimal amount;          // 거래 금액
        private String description;         // 거래 설명
        private LocalDateTime processedAt;  // 처리 시간
        private BigDecimal balanceAfter;    // 거래 후 잔액
        
        public static MileageTransactionInfo from(com.sudo.railo.payment.domain.entity.MileageTransaction transaction) {
            return MileageTransactionInfo.builder()
                    .transactionId(transaction.getId())
                    .transactionType(transaction.getType().getDescription())
                    .amount(transaction.getPointsAmount())
                    .description(transaction.getDescription())
                    .processedAt(transaction.getProcessedAt())
                    .balanceAfter(transaction.getBalanceAfter())
                    .build();
        }
    }
    
    /**
     * 할인 적용 내역 정보
     */
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DiscountInfo {
        
        private String discountType;        // 할인 유형 (쿠폰, 마일리지, 프로모션 등)
        private String discountName;        // 할인명
        private BigDecimal discountAmount;  // 할인 금액
        private String description;         // 할인 설명
        
        /**
         * 마일리지 할인 정보 생성
         */
        public static DiscountInfo createMileageDiscount(BigDecimal mileageUsed, BigDecimal discountAmount) {
            return DiscountInfo.builder()
                    .discountType("MILEAGE")
                    .discountName("마일리지 사용")
                    .discountAmount(discountAmount)
                    .description(String.format("%s포인트 사용 (1포인트 = 1원)", mileageUsed))
                    .build();
        }
        
        /**
         * 일반 프로모션 할인 정보 생성
         */
        public static DiscountInfo createPromotionDiscount(String promotionName, BigDecimal discountAmount, String description) {
            return DiscountInfo.builder()
                    .discountType("PROMOTION")
                    .discountName(promotionName)
                    .discountAmount(discountAmount)
                    .description(description)
                    .build();
        }
    }
    
    /**
     * 결제 요약 정보
     */
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PaymentSummary {
        
        private BigDecimal originalAmount;      // 원본 금액
        private BigDecimal totalDiscount;      // 총 할인 금액
        private BigDecimal finalAmount;        // 최종 결제 금액
        private Integer discountCount;         // 적용된 할인 개수
        private BigDecimal savingsAmount;      // 절약한 금액
        private String savingsRate;           // 절약률 (%)
        
        /**
         * 결제 정보로부터 요약 생성
         */
        public static PaymentSummary from(PaymentInfoResponse payment) {
            BigDecimal originalAmount = payment.getAmountOriginalTotal();
            BigDecimal totalDiscount = payment.getTotalDiscountAmountApplied()
                    .add(payment.getMileageAmountDeducted() != null ? payment.getMileageAmountDeducted() : BigDecimal.ZERO);
            BigDecimal finalAmount = payment.getAmountPaid();
            
            // 절약률 계산
            String savingsRate = "0%";
            if (originalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rate = totalDiscount.divide(originalAmount, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal("100"));
                savingsRate = String.format("%.1f%%", rate);
            }
            
            return PaymentSummary.builder()
                    .originalAmount(originalAmount)
                    .totalDiscount(totalDiscount)
                    .finalAmount(finalAmount)
                    .discountCount(payment.getDiscountDetails() != null ? payment.getDiscountDetails().size() : 0)
                    .savingsAmount(totalDiscount)
                    .savingsRate(savingsRate)
                    .build();
        }
    }
} 