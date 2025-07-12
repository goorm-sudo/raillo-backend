package com.sudo.railo.payment.infrastructure.adapter.out.persistence;

import com.sudo.railo.payment.application.port.out.LoadRefundCalculationPort;
import com.sudo.railo.payment.application.port.out.SaveRefundCalculationPort;
import com.sudo.railo.payment.domain.entity.RefundCalculation;
import com.sudo.railo.payment.domain.repository.RefundCalculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 환불 계산 영속성 어댑터
 * 
 * 애플리케이션 계층의 포트를 구현하여 실제 데이터베이스 접근을 담당
 * 헥사고날 아키텍처의 아웃바운드 어댑터
 */
@Component
@RequiredArgsConstructor
public class RefundCalculationPersistenceAdapter implements LoadRefundCalculationPort, SaveRefundCalculationPort {
    
    private final RefundCalculationRepository refundCalculationRepository;
    
    @Override
    public Optional<RefundCalculation> findByPaymentId(Long paymentId) {
        return refundCalculationRepository.findByPaymentId(paymentId);
    }
    
    @Override
    public List<RefundCalculation> findByPaymentIds(List<Long> paymentIds) {
        return refundCalculationRepository.findByPaymentIds(paymentIds);
    }
    
    @Override
    public List<RefundCalculation> findByMemberId(Long memberId) {
        return refundCalculationRepository.findByMemberId(memberId);
    }
    
    @Override
    public RefundCalculation save(RefundCalculation refundCalculation) {
        return refundCalculationRepository.save(refundCalculation);
    }
}