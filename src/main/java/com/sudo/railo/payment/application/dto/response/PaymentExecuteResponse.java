package com.sudo.railo.payment.application.dto.response;

import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentExecuteResponse {
    
    private Long paymentId;
    private Long reservationId;
    private String externalOrderId;
    private PaymentExecutionStatus paymentStatus;
    private BigDecimal amountPaid;
    
    // 마일리지 관련 정보
    @Builder.Default
    private BigDecimal mileagePointsUsed = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal mileageAmountDeducted = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal mileageToEarn = BigDecimal.ZERO;
    
    // PG 관련 정보
    private String pgTransactionId;
    private String pgApprovalNo;
    private String receiptUrl;
    private LocalDateTime paidAt;
    
    private PaymentResult result;
    
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PaymentResult {
        private boolean success;
        private String message;
        private String errorCode;
        private Map<String, Object> additionalData;
    }
    
    /**
     * ID 조회 (하위 호환성)
     */
    public Long getId() {
        return paymentId;
    }
    
    /**
     * 사용된 마일리지 조회 (하위 호환성)
     * getMileagePointsUsed()의 별칭
     */
    public BigDecimal getMileageUsed() {
        return mileagePointsUsed;
    }
    
    /**
     * 적립될 마일리지 조회 (하위 호환성)
     * getMileageToEarn()의 별칭
     */
    public BigDecimal getMileageEarned() {
        return mileageToEarn;
    }
} 