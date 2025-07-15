package com.sudo.railo.payment.application.port.out;

import com.sudo.railo.payment.domain.entity.RefundCalculation;

import java.util.List;
import java.util.Optional;

/**
 * 환불 계산 조회 포트
 * 
 * 애플리케이션 계층에서 환불 계산 정보를 조회하기 위한 출력 포트
 * 인프라 계층에서 구현
 */
public interface LoadRefundCalculationPort {
    
    Optional<RefundCalculation> findByPaymentId(Long paymentId);
    
    List<RefundCalculation> findByPaymentIds(List<Long> paymentIds);
    
    List<RefundCalculation> findByMemberId(Long memberId);
}