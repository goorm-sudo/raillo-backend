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
 * 카카오페이 Mock Gateway
 */
@Slf4j
@Component
@Profile({"local", "test", "mock"})
public class MockKakaoPayGateway implements PgPaymentGateway {
    
    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.KAKAO_PAY == paymentMethod;
    }
    
    @Override
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        log.info("[Mock 카카오페이] 결제 요청: orderId={}, amount={}", 
                request.getMerchantOrderId(), request.getAmount());
        
        String mockTid = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(mockTid)
                .merchantOrderId(request.getMerchantOrderId())
                .amount(request.getAmount())
                .status("READY")
                .paymentUrl(null)
                .rawResponse("Mock Kakao Pay Response")
                .build();
    }
    
    @Override
    public PgPaymentResponse approvePayment(String pgTransactionId, String merchantOrderId) {
        log.info("[Mock 카카오페이] 결제 승인: tid={}, orderId={}", pgTransactionId, merchantOrderId);
        
        String mockApprovalNumber = "A" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .merchantOrderId(merchantOrderId)
                .status("SUCCESS")
                .approvalNumber(mockApprovalNumber)
                .approvedAt(LocalDateTime.now())
                .rawResponse("Mock Kakao Pay Approve Response")
                .build();
    }
    
    @Override
    public PgPaymentCancelResponse cancelPayment(PgPaymentCancelRequest request) {
        log.info("[Mock 카카오페이] 결제 취소: tid={}, amount={}", 
                request.getPgTransactionId(), request.getCancelAmount());
        
        String mockCancelApprovalNumber = "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        return PgPaymentCancelResponse.success(
                request.getPgTransactionId(),
                request.getMerchantOrderId(),
                request.getCancelAmount(),
                mockCancelApprovalNumber
        );
    }
    
    @Override
    public PgPaymentResponse getPaymentStatus(String pgTransactionId) {
        log.info("[Mock 카카오페이] 결제 상태 조회: tid={}", pgTransactionId);
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .status("SUCCESS")
                .rawResponse("Mock Kakao Pay Status Response")
                .build();
    }
} 