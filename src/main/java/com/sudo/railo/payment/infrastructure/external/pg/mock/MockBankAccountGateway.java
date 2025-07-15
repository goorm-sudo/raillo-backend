package com.sudo.railo.payment.infrastructure.external.pg.mock;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.infrastructure.external.pg.PgPaymentGateway;
import com.sudo.railo.payment.infrastructure.external.pg.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 은행 계좌 결제(내 통장 결제) Mock Gateway
 * 운영 환경에서는 각 은행의 오픈뱅킹 API와 연동
 */
@Slf4j
@Component
public class MockBankAccountGateway implements PgPaymentGateway {
    
    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.BANK_ACCOUNT;
    }
    
    @Override
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        log.debug("[Mock 은행계좌] 결제 요청: orderId={}, amount={}", 
                request.getMerchantOrderId(), request.getAmount());
        
        String mockPaymentId = "B" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        
        // Mock 은행계좌는 바로 승인 처리 (운영에서는 은행 앱으로 리다이렉트)
        log.debug("[Mock 은행계좌] Mock 결제 - 리다이렉트 없이 바로 승인 처리");
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(mockPaymentId)
                .merchantOrderId(request.getMerchantOrderId())
                .amount(request.getAmount())
                .status("READY")
                .paymentUrl(null) // null이면 바로 승인 처리
                .rawResponse("Mock Bank Account Response")
                .build();
    }
    
    @Override
    public PgPaymentResponse approvePayment(String pgTransactionId, String merchantOrderId) {
        log.debug("[Mock 은행계좌] 결제 승인: tid={}, orderId={}", pgTransactionId, merchantOrderId);
        
        // Mock 환경에서는 항상 성공 처리
        boolean isSuccess = true;
        
        if (isSuccess) {
            log.debug("[Mock 은행계좌] 결제 승인 성공: tid={}", pgTransactionId);
            
            return PgPaymentResponse.builder()
                    .success(true)
                    .pgTransactionId(pgTransactionId)
                    .merchantOrderId(merchantOrderId)
                    .status("SUCCESS")
                    .approvedAt(LocalDateTime.now())
                    .rawResponse("Mock Bank Account Approval Success")
                    .build();
        } else {
            log.warn("[Mock 은행계좌] 결제 승인 실패: tid={}", pgTransactionId);
            
            return PgPaymentResponse.builder()
                    .success(false)
                    .pgTransactionId(pgTransactionId)
                    .merchantOrderId(merchantOrderId)
                    .status("FAILED")
                    .errorCode("BANK_ACCOUNT_INVALID")
                    .errorMessage("계좌번호 또는 비밀번호가 올바르지 않습니다.")
                    .rawResponse("Mock Bank Account Approval Failed")
                    .build();
        }
    }
    
    @Override
    public PgPaymentCancelResponse cancelPayment(PgPaymentCancelRequest request) {
        log.debug("[Mock 은행계좌] 결제 취소: tid={}", request.getPgTransactionId());
        
        String mockCancelApprovalNumber = "BC" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        return PgPaymentCancelResponse.success(
                request.getPgTransactionId(),
                "BTX" + UUID.randomUUID().toString().substring(0, 8), // cancelTransactionId
                mockCancelApprovalNumber,
                request.getMerchantOrderId(),
                request.getCancelAmount(),
                request.getCancelReason() != null ? request.getCancelReason() : "고객 요청"
        );
    }
    
    @Override
    public PgPaymentResponse getPaymentStatus(String pgTransactionId) {
        log.debug("[Mock 은행계좌] 결제 상태 조회: tid={}", pgTransactionId);
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .status("SUCCESS")
                .rawResponse("Mock Bank Account Status Success")
                .build();
    }
} 