package com.sudo.railo.payment.infrastructure.external.pg.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * PG사 결제 취소 요청 DTO
 */
@Data
@Builder
public class PgPaymentCancelRequest {
    
    /**
     * PG사 거래 ID
     */
    private String pgTransactionId;
    
    /**
     * 가맹점 주문 번호
     */
    private String merchantOrderId;
    
    /**
     * 취소 금액 (부분 취소 가능)
     */
    private BigDecimal cancelAmount;
    
    /**
     * 취소 사유
     */
    private String cancelReason;
    
    /**
     * 요청자 정보
     */
    private String requestedBy;
} 