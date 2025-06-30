package com.sudo.railo.payment.application.dto.response;

import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 내역 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentHistoryResponse {
    
    private List<PaymentHistoryItem> payments;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    
    /**
     * 결제 내역 아이템
     */
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PaymentHistoryItem {
        
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
        
        // 결제 정보
        private PaymentExecutionStatus paymentStatus;
        private String paymentMethod;
        private String pgProvider;
        private String pgApprovalNo;
        
        // 시간 정보
        private LocalDateTime paidAt;
        private LocalDateTime createdAt;
        
        // 마일리지 요약 정보
        private MileageSummary mileageSummary;
        
        /**
         * Payment 엔티티에서 PaymentHistoryItem 생성
         */
        public static PaymentHistoryItem from(Payment payment, List<MileageTransaction> mileageTransactions) {
            
            // 마일리지 요약 정보 생성
            MileageSummary mileageSummary = MileageSummary.from(mileageTransactions);
            
            return PaymentHistoryItem.builder()
                    .paymentId(payment.getPaymentId())
                    .reservationId(payment.getReservationId())
                    .externalOrderId(payment.getExternalOrderId())
                    .amountOriginalTotal(payment.getAmountOriginalTotal())
                    .totalDiscountAmountApplied(payment.getTotalDiscountAmountApplied())
                    .mileagePointsUsed(payment.getMileagePointsUsed())
                    .mileageAmountDeducted(payment.getMileageAmountDeducted())
                    .mileageToEarn(payment.getMileageToEarn())
                    .amountPaid(payment.getAmountPaid())
                    .paymentStatus(payment.getPaymentStatus())
                    .paymentMethod(payment.getPaymentMethod().getDisplayName())
                    .pgProvider(payment.getPgProvider())
                    .pgApprovalNo(payment.getPgApprovalNo())
                    .paidAt(payment.getPaidAt())
                    .createdAt(payment.getCreatedAt())
                    .mileageSummary(mileageSummary)
                    .build();
        }
    }
    
    /**
     * 마일리지 요약 정보
     */
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class MileageSummary {
        
        private Integer totalTransactions;      // 총 거래 건수
        private BigDecimal totalEarned;         // 총 적립 포인트
        private BigDecimal totalUsed;           // 총 사용 포인트
        private BigDecimal netAmount;           // 순 증감 (적립 - 사용)
        private LocalDateTime lastTransactionAt; // 마지막 거래 시간
        
        /**
         * MileageTransaction 리스트에서 요약 정보 생성
         */
        public static MileageSummary from(List<MileageTransaction> transactions) {
            if (transactions == null || transactions.isEmpty()) {
                return MileageSummary.builder()
                        .totalTransactions(0)
                        .totalEarned(BigDecimal.ZERO)
                        .totalUsed(BigDecimal.ZERO)
                        .netAmount(BigDecimal.ZERO)
                        .build();
            }
            
            BigDecimal totalEarned = transactions.stream()
                    .filter(t -> t.getType() == MileageTransaction.TransactionType.EARN)
                    .map(MileageTransaction::getPointsAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalUsed = transactions.stream()
                    .filter(t -> t.getType() == MileageTransaction.TransactionType.USE)
                    .map(MileageTransaction::getPointsAmount)
                    .map(BigDecimal::abs) // 사용은 음수로 저장되므로 절댓값
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            LocalDateTime lastTransactionAt = transactions.stream()
                    .map(MileageTransaction::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            
            return MileageSummary.builder()
                    .totalTransactions(transactions.size())
                    .totalEarned(totalEarned)
                    .totalUsed(totalUsed)
                    .netAmount(totalEarned.subtract(totalUsed))
                    .lastTransactionAt(lastTransactionAt)
                    .build();
        }
    }
} 