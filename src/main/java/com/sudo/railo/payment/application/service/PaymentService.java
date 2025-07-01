package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.application.dto.response.PaymentExecuteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 결제 서비스 Facade
 * Command/Query 분리 적용 - 단순 위임 역할만 담당
 */
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentExecutionService executionService;
    private final PaymentQueryService queryService;
    
    public PaymentExecuteResponse executePayment(PaymentExecuteRequest request) {
        return executionService.execute(request);
    }
    
    public PaymentExecuteResponse getPayment(Long paymentId) {
        return queryService.getPayment(paymentId);
    }
} 