package com.sudo.railo.payment.infrastructure.persistence;

import com.sudo.railo.payment.domain.entity.SavedPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 저장된 결제수단 Repository
 */
@Repository
public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, Long> {
    
    List<SavedPaymentMethod> findByMemberIdAndIsActiveTrue(Long memberId);
    
    @Modifying
    @Transactional
    @Query("UPDATE SavedPaymentMethod s SET s.isDefault = false WHERE s.memberId = :memberId AND s.isActive = true")
    void updateAllToNonDefault(@Param("memberId") Long memberId);
} 