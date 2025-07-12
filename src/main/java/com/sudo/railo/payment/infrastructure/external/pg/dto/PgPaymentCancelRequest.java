package com.sudo.railo.payment.infrastructure.external.pg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PG 결제 취소/환불 요청 DTO
 * 
 * Mock PG 시스템에서 사용하는 결제 취소 및 환불 요청 정보
 * 실제 PG 연동 시 각 PG사별 요구사항에 맞게 수정 필요
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PgPaymentCancelRequest {
    
    /**
     * 원본 PG 트랜잭션 ID
     */
    private String pgTransactionId;
    
    /**
     * 가맹점 주문 ID
     */
    private String merchantOrderId;
    
    /**
     * 취소/환불 금액
     */
    private BigDecimal cancelAmount;
    
    /**
     * 취소/환불 사유
     */
    private String cancelReason;
    
    /**
     * 취소 타입 (FULL: 전체 취소)
     */
    private String cancelType;
    
    /**
     * 환불 계좌 정보 (계좌 환불 시)
     */
    private String refundBankCode;
    private String refundAccountNumber;
    private String refundAccountHolder;
    
    /**
     * 요청자 정보
     */
    private String requesterName;
    private String requesterPhone;
    
    /**
     * 추가 정보 (각 PG사별 특수 요구사항)
     */
    private Map<String, Object> additionalInfo;
    
    /**
     * 요청자 ID 또는 식별자
     */
    private String requestedBy;
    
    /**
     * 전체 취소 요청 생성 헬퍼 메서드
     */
    public static PgPaymentCancelRequest fullCancel(String pgTransactionId, String merchantOrderId, 
                                                   BigDecimal amount, String reason) {
        return PgPaymentCancelRequest.builder()
                .pgTransactionId(pgTransactionId)
                .merchantOrderId(merchantOrderId)
                .cancelAmount(amount)
                .cancelReason(reason)
                .cancelType("FULL")
                .build();
    }
} 