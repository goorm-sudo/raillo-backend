package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.response.PaymentExecuteResponse;
import com.sudo.railo.payment.application.port.out.LoadPaymentPort;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 조회 전용 서비스
 * Query 패턴 적용 - 결제 조회만 담당
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryService {
    
    private final LoadPaymentPort loadPaymentPort;
    
    public PaymentExecuteResponse getPayment(Long paymentId) {
        Payment payment = loadPaymentPort.findById(paymentId)
            .orElseThrow(() -> new PaymentValidationException("결제 정보를 찾을 수 없습니다"));
        
        return PaymentExecuteResponse.builder()
            .paymentId(payment.getId())
            .externalOrderId(payment.getExternalOrderId())
            .paymentStatus(payment.getPaymentStatus())
            .amountPaid(payment.getAmountPaid())
            .mileagePointsUsed(payment.getMileagePointsUsed())
            .mileageAmountDeducted(payment.getMileageAmountDeducted())
            .mileageToEarn(payment.getMileageToEarn())
            .pgTransactionId(payment.getPgTransactionId())
            .pgApprovalNo(payment.getPgApprovalNo())
            .receiptUrl(payment.getReceiptUrl())
            .paidAt(payment.getPaidAt())
            .build();
    }
}