package com.sudo.railo.payment.infrastructure.external.pg;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.infrastructure.external.pg.dto.*;

/**
 * PG(Payment Gateway) 연동 인터페이스
 * 
 * Strategy 패턴으로 각 PG사별 구현체를 제공
 * 현재는 Mock 구현으로 실제 PG API 호출 없이 시뮬레이션
 */
public interface PgPaymentGateway {
    
    /**
     * 지원하는 결제 수단인지 확인
     * 
     * @param paymentMethod 결제 수단
     * @return 지원 여부
     */
    boolean supports(PaymentMethod paymentMethod);
    
    /**
     * 결제 요청
     * 
     * @param request 결제 요청 정보
     * @return 결제 응답
     */
    PgPaymentResponse requestPayment(PgPaymentRequest request);
    
    /**
     * 결제 승인
     * 
     * @param pgTransactionId PG 트랜잭션 ID
     * @param merchantOrderId 가맹점 주문 ID
     * @return 결제 승인 응답
     */
    PgPaymentResponse approvePayment(String pgTransactionId, String merchantOrderId);
    
    /**
     * 결제 취소/환불
     * 
     * @param request 취소 요청 정보
     * @return 취소 응답
     */
    PgPaymentCancelResponse cancelPayment(PgPaymentCancelRequest request);
    
    /**
     * 결제 상태 조회
     * 
     * @param pgTransactionId PG 트랜잭션 ID
     * @return 결제 상태 응답
     */
    PgPaymentResponse getPaymentStatus(String pgTransactionId);
} 