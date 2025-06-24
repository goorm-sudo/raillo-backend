package com.sudo.railo.payment.domain.repository;

import com.sudo.railo.payment.domain.entity.Payment;
import java.util.List;
import java.util.Optional;

/**
 * Payment 도메인 리포지토리 인터페이스
 * 도메인 관점에서 필요한 데이터 접근 메서드를 정의
 */
public interface PaymentRepository {
    
    /**
     * 결제 정보 저장
     */
    Payment save(Payment payment);
    
    /**
     * ID로 결제 정보 조회
     */
    Optional<Payment> findById(Long paymentId);
    
    /**
     * 예약 ID로 결제 정보 조회
     */
    Optional<Payment> findByReservationId(String reservationId);
    
    /**
     * 외부 주문 ID로 결제 정보 조회
     */
    Optional<Payment> findByExternalOrderId(String externalOrderId);
    
    /**
     * 회원 ID로 결제 목록 조회
     */
    List<Payment> findByMemberId(Long memberId);
    
    /**
     * 비회원 정보로 결제 조회
     */
    Optional<Payment> findByNonMemberNameAndNonMemberPhone(String name, String phone);
    
    /**
     * 멱등성 키 존재 여부 확인
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    /**
     * 멱등성 키로 결제 정보 조회
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
} 