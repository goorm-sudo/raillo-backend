package com.sudo.railo.payment.infrastructure.external.pg.mock;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.infrastructure.external.pg.PgPaymentGateway;
import com.sudo.railo.payment.infrastructure.external.pg.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 계좌이체 결제 Mock Gateway
 * 운영 환경에서는 은행 가상계좌 시스템과 연동
 */
@Slf4j
@Component
public class MockBankTransferGateway implements PgPaymentGateway {
    
    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.BANK_TRANSFER;
    }
    
    @Override
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        log.debug("[Mock 계좌이체] 결제 요청: orderId={}, amount={}", 
                request.getMerchantOrderId(), request.getAmount());
        
        String mockPaymentId = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        
        // Mock 계좌이체는 가상계좌 발급 후 입금 대기 상태
        log.debug("[Mock 계좌이체] 가상계좌 발급 완료 - 입금 대기 상태");
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(mockPaymentId)
                .merchantOrderId(request.getMerchantOrderId())
                .amount(request.getAmount())
                .status("WAITING_FOR_DEPOSIT") // 입금 대기 상태
                .paymentUrl(null) // 별도 URL 없음
                .rawResponse("Mock Bank Transfer - Virtual Account Issued")
                .build();
    }
    
    @Override
    public PgPaymentResponse approvePayment(String pgTransactionId, String merchantOrderId) {
        log.debug("[Mock 계좌이체] 입금 확인 및 결제 승인: tid={}, orderId={}", pgTransactionId, merchantOrderId);
        
        // 입금 확인 시뮬레이션 (95% 성공률 - 입금 확인됨)
        boolean isSuccess = Math.random() > 0.05;
        
        if (isSuccess) {
            log.debug("[Mock 계좌이체] 입금 확인 완료 - 결제 승인 성공: tid={}", pgTransactionId);
            
            return PgPaymentResponse.builder()
                    .success(true)
                    .pgTransactionId(pgTransactionId)
                    .merchantOrderId(merchantOrderId)
                    .status("SUCCESS")
                    .approvedAt(LocalDateTime.now())
                    .rawResponse("Mock Bank Transfer - Deposit Confirmed")
                    .build();
        } else {
            log.warn("[Mock 계좌이체] 입금 확인 실패: tid={}", pgTransactionId);
            
            return PgPaymentResponse.builder()
                    .success(false)
                    .pgTransactionId(pgTransactionId)
                    .merchantOrderId(merchantOrderId)
                    .status("FAILED")
                    .errorCode("DEPOSIT_NOT_CONFIRMED")
                    .errorMessage("입금이 확인되지 않았습니다. 계좌번호와 입금금액을 다시 확인해주세요.")
                    .rawResponse("Mock Bank Transfer - Deposit Not Confirmed")
                    .build();
        }
    }
    
    @Override
    public PgPaymentCancelResponse cancelPayment(PgPaymentCancelRequest request) {
        log.debug("[Mock 계좌이체] 결제 취소: tid={}", request.getPgTransactionId());
        
        return PgPaymentCancelResponse.builder()
                .success(true)
                .pgTransactionId(request.getPgTransactionId())
                .canceledAt(LocalDateTime.now())
                .build();
    }
    
    @Override
    public PgPaymentResponse getPaymentStatus(String pgTransactionId) {
        log.debug("[Mock 계좌이체] 결제 상태 조회: tid={}", pgTransactionId);
        
        // 입금 대기 중인 상태로 응답 (운영에서는 은행에서 입금 상태 확인)
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .status("WAITING_FOR_DEPOSIT")
                .rawResponse("Mock Bank Transfer - Waiting for Deposit")
                .build();
    }
} 