package com.sudo.railo.payment.infrastructure.persistence;

import com.sudo.railo.payment.domain.entity.CalculationStatus;
import com.sudo.railo.payment.domain.entity.PaymentCalculation;
import com.sudo.railo.payment.domain.repository.PaymentCalculationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA 기반 PaymentCalculationRepository 구현체
 * 도메인 리포지토리 인터페이스를 JPA로 구현
 */
@Repository
public interface JpaPaymentCalculationRepository extends JpaRepository<PaymentCalculation, String>, PaymentCalculationRepository {
    
    @Override
    @Query("SELECT pc FROM PaymentCalculation pc WHERE pc.externalOrderId = :externalOrderId")
    Optional<PaymentCalculation> findByExternalOrderId(@Param("externalOrderId") String externalOrderId);
    
    @Override
    @Query("SELECT pc FROM PaymentCalculation pc WHERE pc.userIdExternal = :userId AND pc.status = :status")
    List<PaymentCalculation> findByUserIdExternalAndStatus(@Param("userId") String userId, @Param("status") CalculationStatus status);
    
    @Override
    @Query("SELECT pc FROM PaymentCalculation pc WHERE pc.expiresAt < :expireTime AND pc.status = :status")
    List<PaymentCalculation> findByExpiresAtBeforeAndStatus(@Param("expireTime") LocalDateTime expireTime, 
                                                           @Param("status") CalculationStatus status);
    
    @Override
    @Modifying
    @Query("UPDATE PaymentCalculation pc SET pc.status = :newStatus WHERE pc.calculationId IN :calculationIds")
    void updateStatusByIds(@Param("calculationIds") List<String> calculationIds, @Param("newStatus") CalculationStatus newStatus);
    
    @Override
    @Modifying
    @Query("DELETE FROM PaymentCalculation pc WHERE pc.expiresAt < :expireTime AND pc.status = :status")
    void deleteByExpiresAtBeforeAndStatus(@Param("expireTime") LocalDateTime expireTime, @Param("status") CalculationStatus status);
} 