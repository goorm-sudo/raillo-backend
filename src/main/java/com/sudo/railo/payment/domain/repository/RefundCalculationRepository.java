package com.sudo.railo.payment.domain.repository;

import com.sudo.railo.payment.domain.entity.RefundCalculation;
import com.sudo.railo.payment.domain.entity.RefundStatus;
import com.sudo.railo.payment.domain.entity.RefundType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 환불 계산 Repository 인터페이스 (Domain 계층)
 */
public interface RefundCalculationRepository {
    
    /**
     * 환불 계산 저장
     */
    RefundCalculation save(RefundCalculation refundCalculation);
    
    /**
     * ID로 환불 계산 조회
     */
    Optional<RefundCalculation> findById(Long refundCalculationId);
    
    /**
     * 결제 ID로 환불 계산 조회
     */
    Optional<RefundCalculation> findByPaymentId(Long paymentId);
    
    /**
     * 여러 결제 ID로 환불 계산 목록 조회
     */
    List<RefundCalculation> findByPaymentIds(List<Long> paymentIds);
    
    /**
     * 예약 ID로 환불 계산 조회
     */
    Optional<RefundCalculation> findByReservationId(Long reservationId);
    
    /**
     * 회원 ID로 환불 계산 목록 조회
     */
    List<RefundCalculation> findByMemberId(Long memberId);
    
    /**
     * 환불 상태별 조회
     */
    List<RefundCalculation> findByRefundStatus(RefundStatus refundStatus);
    
    /**
     * 환불 유형별 조회
     */
    List<RefundCalculation> findByRefundType(RefundType refundType);
    
    /**
     * 기간별 환불 계산 조회
     */
    List<RefundCalculation> findByRefundRequestTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 회원별 기간별 환불 계산 조회
     */
    List<RefundCalculation> findByMemberIdAndRefundRequestTimeBetween(
        Long memberId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 처리 대기 중인 환불 계산 조회
     */
    List<RefundCalculation> findPendingRefunds();
    
    /**
     * 환불 계산 삭제
     */
    void delete(RefundCalculation refundCalculation);
    
    /**
     * ID로 환불 계산 삭제
     */
    void deleteById(Long refundCalculationId);
    
    /**
     * 환불 계산 존재 여부 확인
     */
    boolean existsById(Long refundCalculationId);
    
    /**
     * 결제 ID로 환불 계산 존재 여부 확인
     */
    boolean existsByPaymentId(Long paymentId);
    
    /**
     * 예약 ID로 환불 계산 존재 여부 확인
     */
    boolean existsByReservationId(Long reservationId);
    
    /**
     * 멱등성 키로 환불 계산 조회
     */
    Optional<RefundCalculation> findByIdempotencyKey(String idempotencyKey);
} 