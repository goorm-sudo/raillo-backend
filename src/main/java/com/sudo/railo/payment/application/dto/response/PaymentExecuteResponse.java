package com.sudo.railo.payment.application.dto.response;

import com.sudo.railo.payment.domain.entity.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentExecuteResponse {
    
    private Long paymentId;
    private String externalOrderId;
    private PaymentStatus paymentStatus;
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
} 