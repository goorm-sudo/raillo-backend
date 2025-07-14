package com.sudo.railo.payment.infrastructure.persistence;

import com.sudo.railo.payment.domain.entity.RefundCalculation;
import com.sudo.railo.payment.domain.entity.RefundStatus;
import com.sudo.railo.payment.domain.entity.RefundType;
import com.sudo.railo.payment.domain.repository.RefundCalculationRepository;
import com.sudo.railo.payment.infrastructure.persistence.RefundCalculationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 환불 계산 JPA Repository (Infrastructure 계층)
 */
@Repository
@RequiredArgsConstructor
public class JpaRefundCalculationRepository implements RefundCalculationRepository {
    
    private final RefundCalculationJpaRepository jpaRepository;
    
    @Override
    public RefundCalculation save(RefundCalculation refundCalculation) {
        return jpaRepository.save(refundCalculation);
    }
    
    @Override
    public Optional<RefundCalculation> findById(Long refundCalculationId) {
        return jpaRepository.findById(refundCalculationId);
    }
    
    @Override
    public Optional<RefundCalculation> findByPaymentId(Long paymentId) {
        return jpaRepository.findByPaymentId(paymentId);
    }
    
    @Override
    public Optional<RefundCalculation> findByReservationId(Long reservationId) {
        return jpaRepository.findByReservationId(reservationId);
    }
    
    @Override
    public List<RefundCalculation> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }
    
    @Override
    public List<RefundCalculation> findByRefundStatus(RefundStatus refundStatus) {
        return jpaRepository.findByRefundStatusOrderByCreatedAtDesc(refundStatus);
    }
    
    @Override
    public List<RefundCalculation> findByRefundType(RefundType refundType) {
        return jpaRepository.findByRefundTypeOrderByCreatedAtDesc(refundType);
    }
    
    @Override
    public List<RefundCalculation> findByRefundRequestTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return jpaRepository.findByRefundRequestTimeBetweenOrderByRefundRequestTimeDesc(startTime, endTime);
    }
    
    @Override
    public List<RefundCalculation> findByMemberIdAndRefundRequestTimeBetween(
            Long memberId, LocalDateTime startTime, LocalDateTime endTime) {
        return jpaRepository.findByMemberIdAndRefundRequestTimeBetweenOrderByRefundRequestTimeDesc(
            memberId, startTime, endTime);
    }
    
    @Override
    public List<RefundCalculation> findPendingRefunds() {
        return jpaRepository.findByRefundStatusOrderByCreatedAtAsc(RefundStatus.PENDING);
    }
    
    @Override
    public void delete(RefundCalculation refundCalculation) {
        jpaRepository.delete(refundCalculation);
    }
    
    @Override
    public void deleteById(Long refundCalculationId) {
        jpaRepository.deleteById(refundCalculationId);
    }
    
    @Override
    public boolean existsById(Long refundCalculationId) {
        return jpaRepository.existsById(refundCalculationId);
    }
    
    @Override
    public boolean existsByPaymentId(Long paymentId) {
        return jpaRepository.existsByPaymentId(paymentId);
    }
    
    @Override
    public boolean existsByReservationId(Long reservationId) {
        return jpaRepository.existsByReservationId(reservationId);
    }
    
    @Override
    public List<RefundCalculation> findByPaymentIds(List<Long> paymentIds) {
        return jpaRepository.findByPaymentIds(paymentIds);
    }
    
    @Override
    public Optional<RefundCalculation> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey);
    }
} 