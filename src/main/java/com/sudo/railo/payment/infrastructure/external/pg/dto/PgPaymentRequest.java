package com.sudo.railo.payment.infrastructure.external.pg.dto;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PG 결제 요청 DTO
 * 
 * Mock PG 시스템에서 사용하는 결제 요청 정보
 * 실제 PG 연동 시 각 PG사별 요구사항에 맞게 수정 필요
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PgPaymentRequest {
    
    /**
     * 가맹점 주문 ID (Payment ID 사용)
     */
    private String merchantOrderId;
    
    /**
     * 결제 금액
     */
    private BigDecimal amount;
    
    /**
     * 결제 수단 타입 (CREDIT_CARD, BANK_TRANSFER, MOBILE)
     */
    private String paymentMethodType;
    
    /**
     * PG 제공자 (NICE_PAY, TOSS_PAYMENTS, KG_INICIS)
     */
    private String pgProvider;
    
    /**
     * PG 토큰 (프론트엔드에서 받은 결제 토큰)
     */
    private String pgToken;
    
    /**
     * 주문명
     */
    private String orderName;
    
    /**
     * 구매자 정보
     */
    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;
    
    /**
     * 현금영수증 정보
     */
    private boolean cashReceiptRequested;
    private String cashReceiptType;
    private String cashReceiptNumber;
    
    /**
     * 추가 정보 (각 PG사별 특수 요구사항)
     */
    private Map<String, Object> additionalInfo;
    
    /**
     * 성공 리다이렉트 URL
     */
    private String successUrl;
    
    /**
     * 실패 리다이렉트 URL
     */
    private String failUrl;
    
    /**
     * 취소 리다이렉트 URL
     */
    private String cancelUrl;
    
    /**
     * 상품명
     */
    private String productName;
    
    /**
     * 결제 방법 (PaymentMethod enum)
     */
    private PaymentMethod paymentMethod;
} 