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
 * 네이버페이 Mock Gateway (테스트용)
 * PG 연동 전 개발/테스트를 위한 Mock 구현
 */
@Slf4j
@Component
@Profile({"local", "test", "mock"}) // Mock 환경에서만 활성화
public class MockNaverPayGateway implements PgPaymentGateway {
    
    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.NAVER_PAY == paymentMethod;
    }
    
    @Override
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        log.debug("[Mock 네이버페이] 결제 요청: orderId={}, amount={}", 
                request.getMerchantOrderId(), request.getAmount());
        
        String mockPaymentId = "N" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        // Mock 환경에서는 URL 대신 null 반환하여 프론트엔드에서 바로 완료 처리
        String mockPaymentUrl = null; // 리다이렉트 없이 Mock 처리
        
        log.debug("[Mock 네이버페이] Mock 결제 - 리다이렉트 없이 바로 승인 처리");
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(mockPaymentId)
                .merchantOrderId(request.getMerchantOrderId())
                .amount(request.getAmount())
                .status("READY")
                .paymentUrl(mockPaymentUrl)
                .rawResponse("Mock Naver Pay Response - No Redirect")
                .build();
    }
    
    @Override
    public PgPaymentResponse approvePayment(String pgTransactionId, String merchantOrderId) {
        log.debug("[Mock 네이버페이] 결제 승인: paymentId={}, orderId={}", pgTransactionId, merchantOrderId);
        
        String mockApprovalNumber = "NA" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .merchantOrderId(merchantOrderId)
                .status("SUCCESS")
                .approvalNumber(mockApprovalNumber)
                .approvedAt(LocalDateTime.now())
                .rawResponse("Mock Naver Pay Approve Response")
                .build();
    }
    
    @Override
    public PgPaymentCancelResponse cancelPayment(PgPaymentCancelRequest request) {
        log.debug("[Mock 네이버페이] 결제 취소: paymentId={}, amount={}", 
                request.getPgTransactionId(), request.getCancelAmount());
        
        String mockCancelId = "NC" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        // success 메서드는 6개 파라미터를 요구함
        return PgPaymentCancelResponse.success(
                request.getPgTransactionId(),
                "NTX" + UUID.randomUUID().toString().substring(0, 8), // cancelTransactionId
                mockCancelId,
                request.getMerchantOrderId(),
                request.getCancelAmount(),
                request.getCancelReason() != null ? request.getCancelReason() : "고객 요청"
        );
    }
    
    @Override
    public PgPaymentResponse getPaymentStatus(String pgTransactionId) {
        log.debug("[Mock 네이버페이] 결제 상태 조회: paymentId={}", pgTransactionId);
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .status("SUCCESS")
                .rawResponse("Mock Naver Pay Status Response")
                .build();
    }
} 