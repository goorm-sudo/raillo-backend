package com.sudo.railo.payment.infrastructure.external.pg.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PG사 결제 취소 응답 DTO
 */
@Data
@Builder
public class PgPaymentCancelResponse {
    
    /**
     * 취소 성공 여부
     */
    private boolean success;
    
    /**
     * PG사 거래 ID
     */
    private String pgTransactionId;
    
    /**
     * 가맹점 주문 번호
     */
    private String merchantOrderId;
    
    /**
     * 취소 금액
     */
    private BigDecimal cancelAmount;
    
    /**
     * 취소 승인 번호
     */
    private String cancelApprovalNumber;
    
    /**
     * 취소 완료 시간
     */
    private LocalDateTime canceledAt;
    
    /**
     * 오류 코드
     */
    private String errorCode;
    
    /**
     * 오류 메시지
     */
    private String errorMessage;
    
    /**
     * 성공 응답 생성
     */
    public static PgPaymentCancelResponse success(String pgTransactionId, String merchantOrderId, 
                                                BigDecimal cancelAmount, String cancelApprovalNumber) {
        return PgPaymentCancelResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .merchantOrderId(merchantOrderId)
                .cancelAmount(cancelAmount)
                .cancelApprovalNumber(cancelApprovalNumber)
                .canceledAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 실패 응답 생성
     */
    public static PgPaymentCancelResponse failure(String merchantOrderId, String errorCode, String errorMessage) {
        return PgPaymentCancelResponse.builder()
                .success(false)
                .merchantOrderId(merchantOrderId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
} 