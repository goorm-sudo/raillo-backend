package com.sudo.railo.payment.infrastructure.persistence;

import com.sudo.railo.payment.domain.entity.SavedPaymentMethod;
import com.sudo.railo.payment.domain.repository.SavedPaymentMethodRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 저장된 결제수단 JPA Repository 구현체
 */
@Repository
public interface JpaSavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, Long>, SavedPaymentMethodRepository {
    
    @Override
    List<SavedPaymentMethod> findByMemberIdAndIsActive(Long memberId, Boolean isActive);
    
    @Override
    List<SavedPaymentMethod> findByMemberId(Long memberId);
    
    @Override
    Optional<SavedPaymentMethod> findByMemberIdAndIsDefaultTrue(Long memberId);
    
    @Override
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SavedPaymentMethod s " +
           "WHERE s.memberId = :memberId AND (s.cardNumberHash = :hash OR s.accountNumberHash = :hash) " +
           "AND s.isActive = true")
    boolean existsByMemberIdAndHash(@Param("memberId") Long memberId, @Param("hash") String hash);
    
    @Override
    @Modifying
    @Query("UPDATE SavedPaymentMethod s SET s.isDefault = false WHERE s.memberId = :memberId")
    void updateAllToNotDefault(@Param("memberId") Long memberId);
    
    @Override
    @Query("SELECT COUNT(s) FROM SavedPaymentMethod s " +
           "WHERE s.memberId = :memberId AND s.paymentMethodType = :type AND s.isActive = true")
    long countByMemberIdAndTypeAndActive(@Param("memberId") Long memberId, @Param("type") String type);
}