package com.sudo.railo.payment.application.port.out;

import com.sudo.railo.payment.domain.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 결제 정보 조회 포트
 * 
 * 애플리케이션 계층에서 결제 정보를 조회하기 위한 출력 포트
 * 인프라 계층에서 구현
 */
public interface LoadPaymentPort {
    
    /**
     * ID로 결제 정보 조회
     * 
     * @param paymentId 결제 ID
     * @return 결제 정보
     */
    Optional<Payment> findById(Long paymentId);
    
    /**
     * 멱등성 키로 결제 존재 여부 확인
     * 
     * @param idempotencyKey 멱등성 키
     * @return 존재 여부
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    /**
     * 외부 주문 ID로 결제 조회
     * 
     * @param externalOrderId 외부 주문 ID
     * @return 결제 정보
     */
    Optional<Payment> findByExternalOrderId(String externalOrderId);
    
    /**
     * 예약 ID로 결제 조회
     * 
     * @param reservationId 예약 ID
     * @return 결제 정보
     */
    Optional<Payment> findByReservationId(Long reservationId);
    
    /**
     * 회원 ID로 결제 내역 조회 (페이징)
     * 
     * @param memberId 회원 ID
     * @param pageable 페이징 정보
     * @return 페이징된 결제 내역
     */
    Page<Payment> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
    
    /**
     * 회원 ID와 기간으로 결제 내역 조회 (페이징)
     * 
     * @param memberId 회원 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @param pageable 페이징 정보
     * @return 페이징된 결제 내역
     */
    Page<Payment> findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long memberId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * 비회원 정보로 결제 내역 조회 (페이징)
     * 
     * @param name 비회원 이름
     * @param phoneNumber 비회원 전화번호
     * @param pageable 페이징 정보
     * @return 페이징된 결제 내역
     */
    Page<Payment> findByNonMemberInfo(String name, String phoneNumber, Pageable pageable);
}