package com.sudo.railo.payment.infrastructure.client;

import com.sudo.railo.payment.infrastructure.client.dto.PgVerificationResult;

/**
 * PG사 API 클라이언트 인터페이스
 */
public interface PgApiClient {
    
    /**
     * PG 승인번호 검증
     * 
     * @param authNumber PG 승인번호
     * @param orderId 주문번호
     * @return 검증 결과
     */
    PgVerificationResult verifyPayment(String authNumber, String orderId);
}