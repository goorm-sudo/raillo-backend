package com.sudo.railo.payment.application.dto.response;

import com.sudo.railo.payment.domain.entity.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 정보 응답 DTO (비회원용 및 회원 상세 조회용)
 */
@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentInfoResponse {
    
    private Long paymentId;
    private String reservationId;
    private String externalOrderId;
    
    // 결제 금액 정보
    private BigDecimal amountOriginalTotal;
    private BigDecimal totalDiscountAmountApplied;
    private BigDecimal mileagePointsUsed;
    private BigDecimal mileageAmountDeducted;
    private BigDecimal mileageToEarn;
    private BigDecimal amountPaid;
    
    // 결제 상태 정보
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private String pgProvider;
    private String pgTransactionId;
    private String pgApprovalNo;
    private String receiptUrl;
    
    // 시간 정보
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    
    // 비회원 정보 (마스킹된 형태)
    private String nonMemberName;
    private String nonMemberPhoneMasked;
    
    // 마일리지 거래 내역 (회원 상세 조회시에만 포함)
    private List<MileageTransactionInfo> mileageTransactions;
    
    /**
     * 마일리지 거래 정보
     */
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class MileageTransactionInfo {
        
        private Long transactionId;
        private String type;                    // 거래 유형 (적립, 사용, 만료 등)
        private BigDecimal pointsAmount;        // 포인트 수량
        private BigDecimal balanceAfter;        // 거래 후 잔액
        private String description;             // 거래 설명
        private LocalDateTime processedAt;      // 처리 시간
        private String status;                  // 거래 상태
    }
} 