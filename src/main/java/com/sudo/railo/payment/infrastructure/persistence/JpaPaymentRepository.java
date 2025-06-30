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
    List<Payment> findByMemberId(Long memberId);
    
    @Override
    Page<Payment> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
    
    @Override
    Page<Payment> findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long memberId, 
        LocalDateTime startDate, 
        LocalDateTime endDate, 
        Pageable pageable
    );
    
    @Override
    @Query("SELECT p FROM Payment p WHERE p.nonMemberName = :name AND p.nonMemberPhone = :phone")
    Optional<Payment> findByNonMemberNameAndNonMemberPhone(@Param("name") String name, @Param("phone") String phone);
    
    @Override
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    @Override
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
} 