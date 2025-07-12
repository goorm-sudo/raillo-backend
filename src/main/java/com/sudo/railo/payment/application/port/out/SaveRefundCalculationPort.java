package com.sudo.railo.payment.application.port.out;

import com.sudo.railo.payment.domain.entity.RefundCalculation;

/**
 * 환불 계산 저장 포트
 * 
 * 애플리케이션 계층에서 환불 계산 정보를 저장하기 위한 출력 포트
 * 인프라 계층에서 구현
 */
public interface SaveRefundCalculationPort {
    
    RefundCalculation save(RefundCalculation refundCalculation);
}