package com.sudo.railo.payment.infrastructure.external.pg.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PG사 결제 응답 DTO
 */
@Data
@Builder
public class PgPaymentResponse {
    
    /**
     * 응답 성공 여부
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
     * 결제 금액
     */
    private BigDecimal amount;
    
    /**
     * 결제 상태 (READY, SUCCESS, FAILED, CANCELLED)
     */
    private String status;
    
    /**
     * 결제창 URL (즉시 결제가 아닌 경우)
     */
    private String paymentUrl;
    
    /**
     * 결제 승인 번호
     */
    private String approvalNumber;
    
    /**
     * 결제 승인 시간
     */
    private LocalDateTime approvedAt;
    
    /**
     * 오류 코드
     */
    private String errorCode;
    
    /**
     * 오류 메시지
     */
    private String errorMessage;
    
    /**
     * PG사 원본 응답 (디버깅용)
     */
    private String rawResponse;
    
    /**
     * 성공 응답 생성
     */
    public static PgPaymentResponse success(String pgTransactionId, String merchantOrderId, 
                                          BigDecimal amount, String status) {
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .merchantOrderId(merchantOrderId)
                .amount(amount)
                .status(status)
                .build();
    }
    
    /**
     * 실패 응답 생성
     */
    public static PgPaymentResponse failure(String merchantOrderId, String errorCode, String errorMessage) {
        return PgPaymentResponse.builder()
                .success(false)
                .merchantOrderId(merchantOrderId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
} 