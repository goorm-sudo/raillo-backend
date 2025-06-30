package com.sudo.railo.payment.infrastructure.external.pg;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.infrastructure.external.pg.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PG 연동 통합 서비스
 * Strategy 패턴으로 각 PG사별 Gateway를 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PgPaymentService {
    
    private final List<PgPaymentGateway> pgGateways;
    
    /**
     * 결제 수단에 맞는 PG Gateway 조회
     */
    private PgPaymentGateway getGateway(PaymentMethod paymentMethod) {
        return pgGateways.stream()
                .filter(gateway -> gateway.supports(paymentMethod))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 결제 수단: " + paymentMethod));
    }
    
    /**
     * 결제 요청
     */
    public PgPaymentResponse requestPayment(PaymentMethod paymentMethod, PgPaymentRequest request) {
        log.debug("PG 결제 요청: method={}, orderId={}", paymentMethod, request.getMerchantOrderId());
        
        PgPaymentGateway gateway = getGateway(paymentMethod);
        return gateway.requestPayment(request);
    }
    
    /**
     * 결제 승인
     */
    public PgPaymentResponse approvePayment(PaymentMethod paymentMethod, String pgTransactionId, String merchantOrderId) {
        log.debug("PG 결제 승인: method={}, tid={}, orderId={}", paymentMethod, pgTransactionId, merchantOrderId);
        
        PgPaymentGateway gateway = getGateway(paymentMethod);
        return gateway.approvePayment(pgTransactionId, merchantOrderId);
    }
    
    /**
     * 결제 취소
     */
    public PgPaymentCancelResponse cancelPayment(PaymentMethod paymentMethod, PgPaymentCancelRequest request) {
        log.debug("PG 결제 취소: method={}, tid={}", paymentMethod, request.getPgTransactionId());
        
        PgPaymentGateway gateway = getGateway(paymentMethod);
        return gateway.cancelPayment(request);
    }
    
    /**
     * 결제 상태 조회
     */
    public PgPaymentResponse getPaymentStatus(PaymentMethod paymentMethod, String pgTransactionId) {
        log.debug("PG 결제 상태 조회: method={}, tid={}", paymentMethod, pgTransactionId);
        
        PgPaymentGateway gateway = getGateway(paymentMethod);
        return gateway.getPaymentStatus(pgTransactionId);
    }
    
    /**
     * 외부 PG 연동이 필요한 결제 수단인지 확인
     */
    public boolean requiresExternalPg(PaymentMethod paymentMethod) {
        return paymentMethod.requiresExternalPg();
    }
} 