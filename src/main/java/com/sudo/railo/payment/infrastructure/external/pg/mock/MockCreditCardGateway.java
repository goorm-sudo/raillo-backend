package com.sudo.railo.payment.infrastructure.external.pg.mock;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.infrastructure.external.pg.PgPaymentGateway;
import com.sudo.railo.payment.infrastructure.external.pg.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mock 신용카드 Gateway
 * 신용카드 PG 연동 전까지 테스트용으로 사용
 */
@Slf4j
@Component
@Profile({"local", "test", "mock"}) // Mock 환경에서만 활성화
public class MockCreditCardGateway implements PgPaymentGateway {
    
    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.CREDIT_CARD;
    }
    
    @Override
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        log.info("[Mock 신용카드] 결제 요청: orderId={}, amount={}", 
                request.getMerchantOrderId(), request.getAmount());
        
        String mockPaymentId = "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        
        // Mock 신용카드는 카카오페이/네이버페이와 동일하게 바로 승인 처리
        log.info("[Mock 신용카드] Mock 결제 - 리다이렉트 없이 바로 승인 처리");
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(mockPaymentId)
                .merchantOrderId(request.getMerchantOrderId())
                .amount(request.getAmount())
                .status("READY")
                .paymentUrl(null) // null이면 바로 승인 처리
                .rawResponse("Mock Credit Card Response")
                .build();
    }
    
    @Override
    public PgPaymentResponse approvePayment(String pgTransactionId, String merchantOrderId) {
        log.info("[Mock 신용카드] 결제 승인: tid={}, orderId={}", pgTransactionId, merchantOrderId);
        
        // Mock 승인 처리
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .merchantOrderId(merchantOrderId)
                .status("SUCCESS")
                .approvalNumber("MOCK_CARD_" + System.currentTimeMillis())
                .approvedAt(LocalDateTime.now())
                .rawResponse("Mock Credit Card Approval Success")
                .build();
    }
    
    @Override
    public PgPaymentCancelResponse cancelPayment(PgPaymentCancelRequest request) {
        log.info("[Mock 신용카드] 결제 취소: tid={}, amount={}", 
                request.getPgTransactionId(), request.getCancelAmount());
        
        return PgPaymentCancelResponse.builder()
                .success(true)
                .pgTransactionId(request.getPgTransactionId())
                .merchantOrderId(request.getMerchantOrderId())
                .cancelAmount(request.getCancelAmount())
                .canceledAt(LocalDateTime.now())
                .build();
    }
    
    @Override
    public PgPaymentResponse getPaymentStatus(String pgTransactionId) {
        log.info("[Mock 신용카드] 결제 상태 조회: tid={}", pgTransactionId);
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .status("SUCCESS")
                .rawResponse("Mock Credit Card Status Check")
                .build();
    }
} 