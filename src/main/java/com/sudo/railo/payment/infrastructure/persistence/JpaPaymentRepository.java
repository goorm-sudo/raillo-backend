package com.sudo.railo.payment.infrastructure.persistence;

import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA 기반 PaymentRepository 구현체
 * 도메인 리포지토리 인터페이스를 JPA로 구현
 */
@Repository
public interface JpaPaymentRepository extends JpaRepository<Payment, Long>, PaymentRepository {
    
    @Override
    Optional<Payment> findByReservationId(Long reservationId);
    
    @Override
    Optional<Payment> findByExternalOrderId(String externalOrderId);
    
    @Override
    @Query("SELECT p FROM Payment p WHERE p.member.id = :memberId")
    List<Payment> findByMemberId(@Param("memberId") Long memberId);
    
    @Override
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.member WHERE p.member.id = :memberId ORDER BY p.createdAt DESC")
    Page<Payment> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId, Pageable pageable);
    
    @Override
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.member WHERE p.member.id = :memberId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    Page<Payment> findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        @Param("memberId") Long memberId, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate, 
        Pageable pageable
    );
    
    @Override
    @Query("SELECT p FROM Payment p WHERE p.nonMemberName = :name AND p.nonMemberPhone = :phone")
    Optional<Payment> findByNonMemberNameAndNonMemberPhone(@Param("name") String name, @Param("phone") String phone);
    
    @Override
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    @Override
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * 비회원 정보로 모든 결제 내역 조회 (페이징)
     */
    @Query("SELECT p FROM Payment p WHERE p.nonMemberName = :name AND p.nonMemberPhone = :phone ORDER BY p.createdAt DESC")
    Page<Payment> findByNonMemberNameAndNonMemberPhoneOrderByCreatedAtDesc(
        @Param("name") String name, 
        @Param("phone") String phone, 
        Pageable pageable
    );
    
    @Override
    @Query("SELECT p FROM Payment p WHERE p.reservationId = :reservationId AND p.deletedAt IS NULL")
    Optional<Payment> findByReservationIdAndNotDeleted(@Param("reservationId") Long reservationId);
    
    @Override
    @Query("SELECT p FROM Payment p WHERE p.nonMemberName = :name AND p.nonMemberPhone = :phone AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    default Page<Payment> findByNonMemberInfo(String name, String phone, Pageable pageable) {
        return findByNonMemberNameAndNonMemberPhoneAndDeletedAtIsNull(name, phone, pageable);
    }
    
    @Query("SELECT p FROM Payment p WHERE p.nonMemberName = :name AND p.nonMemberPhone = :phone AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Page<Payment> findByNonMemberNameAndNonMemberPhoneAndDeletedAtIsNull(
        @Param("name") String name, 
        @Param("phone") String phone, 
        Pageable pageable
    );
} 