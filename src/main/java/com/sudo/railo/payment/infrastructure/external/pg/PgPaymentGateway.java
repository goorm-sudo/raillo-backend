package com.sudo.railo.payment.infrastructure.external.pg;

import com.sudo.railo.payment.infrastructure.external.pg.dto.PgPaymentRequest;
import com.sudo.railo.payment.infrastructure.external.pg.dto.PgPaymentResponse;
import com.sudo.railo.payment.infrastructure.external.pg.dto.PgPaymentCancelRequest;
import com.sudo.railo.payment.infrastructure.external.pg.dto.PgPaymentCancelResponse;
import com.sudo.railo.payment.domain.entity.PaymentMethod;

/**
 * PG사 연동을 위한 공통 인터페이스
 * Strategy 패턴으로 각 PG사별 구현체 제공
 */
public interface PgPaymentGateway {
    
    /**
     * 지원하는 결제 수단 확인
     */
    boolean supports(PaymentMethod paymentMethod);
    
    /**
     * 결제 요청 (결제창 URL 생성 또는 즉시 결제)
     * @param request 결제 요청 정보
     * @return 결제 응답 (결제창 URL 또는 결제 결과)
     */
    PgPaymentResponse requestPayment(PgPaymentRequest request);
    
    /**
     * 결제 승인 (결제창에서 돌아온 후 최종 승인)
     * @param pgTransactionId PG사 거래 ID
     * @param merchantOrderId 가맹점 주문 ID
     * @return 결제 승인 결과
     */
    PgPaymentResponse approvePayment(String pgTransactionId, String merchantOrderId);
    
    /**
     * 결제 취소/환불
     * @param request 취소 요청 정보
     * @return 취소 결과
     */
    PgPaymentCancelResponse cancelPayment(PgPaymentCancelRequest request);
    
    /**
     * 결제 상태 조회
     * @param pgTransactionId PG사 거래 ID
     * @return 결제 상태 정보
     */
    PgPaymentResponse getPaymentStatus(String pgTransactionId);
} 