package com.sudo.railo.payment.application.dto;

import com.sudo.railo.payment.domain.entity.Payment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 결제 실행 결과
 * 
 * 결제 실행 후 반환되는 결과를 담은 DTO
 * Payment 엔티티와 관련 실행 결과 정보를 포함
 */
@Getter
@Builder
public class PaymentResult {
    
    /**
     * 저장된 Payment 엔티티
     */
    private final Payment payment;
    
    /**
     * 마일리지 실행 결과
     */
    private final MileageExecutionResult mileageResult;
    
    /**
     * PG 결제 결과
     */
    private final PgPaymentResult pgResult;
    
    /**
     * 성공 여부
     */
    private final boolean success;
    
    /**
     * 결과 메시지
     */
    private final String message;
    
    /**
     * 마일리지 실행 결과
     */
    @Getter
    @Builder
    public static class MileageExecutionResult {
        private final boolean success;
        private final BigDecimal usedPoints;
        private final BigDecimal remainingBalance;
        private final String transactionId;
        
        /**
         * ID 조회 (하위 호환성)
         */
        public String getId() {
            return transactionId;
        }
    }
    
    /**
     * PG 결제 결과
     */
    @Getter
    @Builder
    public static class PgPaymentResult {
        private final boolean success;
        private final String pgTransactionId;
        private final String pgApprovalNo;
        private final String pgMessage;
        
        public static PgPaymentResult success(String pgTransactionId, String pgApprovalNo) {
            return PgPaymentResult.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .pgApprovalNo(pgApprovalNo)
                .build();
        }
        
        public static PgPaymentResult failure(String message) {
            return PgPaymentResult.builder()
                .success(false)
                .pgMessage(message)
                .build();
        }
    }
    
    /**
     * 성공 결과 생성
     */
    public static PaymentResult success(Payment payment,
                                       MileageExecutionResult mileageResult,
                                       PgPaymentResult pgResult) {
        return PaymentResult.builder()
                .payment(payment)
                .mileageResult(mileageResult)
                .pgResult(pgResult)
                .success(true)
                .message("결제가 성공적으로 처리되었습니다")
                .build();
    }
    
    /**
     * 실패 결과 생성
     */
    public static PaymentResult failure(Payment payment, String message) {
        return PaymentResult.builder()
                .payment(payment)
                .success(false)
                .message(message)
                .build();
    }
}