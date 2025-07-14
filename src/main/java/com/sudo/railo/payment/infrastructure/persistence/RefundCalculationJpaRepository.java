package com.sudo.railo.payment.infrastructure.persistence;

import com.sudo.railo.payment.domain.entity.RefundCalculation;
import com.sudo.railo.payment.domain.entity.RefundStatus;
import com.sudo.railo.payment.domain.entity.RefundType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundCalculationJpaRepository extends JpaRepository<RefundCalculation, Long> {
    Optional<RefundCalculation> findByPaymentId(Long paymentId);
    Optional<RefundCalculation> findByReservationId(Long reservationId);
    List<RefundCalculation> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<RefundCalculation> findByRefundStatusOrderByCreatedAtDesc(RefundStatus refundStatus);
    List<RefundCalculation> findByRefundStatusOrderByCreatedAtAsc(RefundStatus refundStatus);
    List<RefundCalculation> findByRefundTypeOrderByCreatedAtDesc(RefundType refundType);
    List<RefundCalculation> findByRefundRequestTimeBetweenOrderByRefundRequestTimeDesc(LocalDateTime startTime, LocalDateTime endTime);
    List<RefundCalculation> findByMemberIdAndRefundRequestTimeBetweenOrderByRefundRequestTimeDesc(Long memberId, LocalDateTime startTime, LocalDateTime endTime);
    boolean existsByPaymentId(Long paymentId);
    boolean existsByReservationId(Long reservationId);
    
    @Query("SELECT rc FROM RefundCalculation rc WHERE rc.paymentId IN :paymentIds")
    List<RefundCalculation> findByPaymentIds(@Param("paymentIds") List<Long> paymentIds);
    
    Optional<RefundCalculation> findByIdempotencyKey(String idempotencyKey);
}
