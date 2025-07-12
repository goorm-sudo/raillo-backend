package com.sudo.railo.payment.infrastructure.external.pg.mock;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.infrastructure.external.pg.PgPaymentGateway;
import com.sudo.railo.payment.infrastructure.external.pg.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PAYCO 결제 Mock Gateway
 * 운영 환경에서는 PAYCO API와 연동
 */
@Slf4j
@Component
public class MockPaycoGateway implements PgPaymentGateway {
    
    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.PAYCO;
    }
    
    @Override
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        log.debug("[Mock PAYCO] 결제 요청: orderId={}, amount={}", 
                request.getMerchantOrderId(), request.getAmount());
        
        String mockPaymentId = "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        
        // Mock PAYCO는 바로 승인 처리 (운영에서는 PAYCO 앱으로 리다이렉트)
        log.debug("[Mock PAYCO] Mock 결제 - 리다이렉트 없이 바로 승인 처리");
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(mockPaymentId)
                .merchantOrderId(request.getMerchantOrderId())
                .amount(request.getAmount())
                .status("READY")
                .paymentUrl(null) // null이면 바로 승인 처리
                .rawResponse("Mock PAYCO Response")
                .build();
    }
    
    @Override
    public PgPaymentResponse approvePayment(String pgTransactionId, String merchantOrderId) {
        log.debug("[Mock PAYCO] 결제 승인: tid={}, orderId={}", pgTransactionId, merchantOrderId);
        
        // 결제 승인 성공 시뮬레이션 (95% 성공률)
        boolean isSuccess = Math.random() > 0.05;
        
        if (isSuccess) {
            log.debug("[Mock PAYCO] 결제 승인 성공: tid={}", pgTransactionId);
            
            return PgPaymentResponse.builder()
                    .success(true)
                    .pgTransactionId(pgTransactionId)
                    .merchantOrderId(merchantOrderId)
                    .status("SUCCESS")
                    .approvedAt(LocalDateTime.now())
                    .rawResponse("Mock PAYCO Approval Success")
                    .build();
        } else {
            log.warn("[Mock PAYCO] 결제 승인 실패: tid={}", pgTransactionId);
            
            return PgPaymentResponse.builder()
                    .success(false)
                    .pgTransactionId(pgTransactionId)
                    .merchantOrderId(merchantOrderId)
                    .status("FAILED")
                    .errorCode("PAYCO_APPROVAL_FAILED")
                    .errorMessage("PAYCO 결제 승인이 실패했습니다.")
                    .rawResponse("Mock PAYCO Approval Failed")
                    .build();
        }
    }
    
         @Override
     public PgPaymentCancelResponse cancelPayment(PgPaymentCancelRequest request) {
         log.debug("[Mock PAYCO] 결제 취소: tid={}", request.getPgTransactionId());
         
         return PgPaymentCancelResponse.builder()
                 .success(true)
                 .pgTransactionId(request.getPgTransactionId())
                 .canceledAt(LocalDateTime.now())
                 .build();
     }
    
    @Override
    public PgPaymentResponse getPaymentStatus(String pgTransactionId) {
        log.debug("[Mock PAYCO] 결제 상태 조회: tid={}", pgTransactionId);
        
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .status("SUCCESS")
                .rawResponse("Mock PAYCO Status Success")
                .build();
    }
} 