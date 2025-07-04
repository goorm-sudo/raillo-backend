package com.sudo.railo.payment.infrastructure.external.pg.dto;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * PG사 결제 요청 DTO
 */
@Data
@Builder
public class PgPaymentRequest {
    
    /**
     * 가맹점 주문 번호 (Raillo 내부 결제 ID)
     */
    private String merchantOrderId;
    
    /**
     * 결제 금액
     */
    private BigDecimal amount;
    
    /**
     * 결제 수단
     */
    private PaymentMethod paymentMethod;
    
    /**
     * 상품명
     */
    private String productName;
    
    /**
     * 구매자 정보
     */
    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;
    
    /**
     * 결제 성공 시 리다이렉트 URL
     */
    private String successUrl;
    
    /**
     * 결제 실패 시 리다이렉트 URL
     */
    private String failUrl;
    
    /**
     * 결제 취소 시 리다이렉트 URL
     */
    private String cancelUrl;
    
    /**
     * 추가 파라미터 (PG사별 특수 옵션)
     */
    private String additionalParams;
} 