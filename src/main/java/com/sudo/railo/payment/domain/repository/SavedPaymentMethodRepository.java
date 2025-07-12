package com.sudo.railo.payment.domain.repository;

import com.sudo.railo.payment.domain.entity.SavedPaymentMethod;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 저장된 결제수단 Repository 인터페이스
 */
public interface SavedPaymentMethodRepository {
    
    /**
     * 저장
     */
    SavedPaymentMethod save(SavedPaymentMethod savedPaymentMethod);
    
    /**
     * ID로 조회
     */
    Optional<SavedPaymentMethod> findById(Long id);
    
    /**
     * 회원 ID와 활성 상태로 조회
     */
    List<SavedPaymentMethod> findByMemberIdAndIsActive(Long memberId, Boolean isActive);
    
    /**
     * 회원 ID로 모든 결제수단 조회
     */
    List<SavedPaymentMethod> findByMemberId(Long memberId);
    
    /**
     * 회원의 기본 결제수단 조회
     */
    Optional<SavedPaymentMethod> findByMemberIdAndIsDefaultTrue(Long memberId);
    
    /**
     * 해시값으로 중복 체크
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SavedPaymentMethod s " +
           "WHERE s.memberId = :memberId AND (s.cardNumberHash = :hash OR s.accountNumberHash = :hash) " +
           "AND s.isActive = true")
    boolean existsByMemberIdAndHash(@Param("memberId") Long memberId, @Param("hash") String hash);
    
    /**
     * 회원의 모든 결제수단을 기본값 해제
     */
    @Modifying
    @Query("UPDATE SavedPaymentMethod s SET s.isDefault = false WHERE s.memberId = :memberId")
    void updateAllToNotDefault(@Param("memberId") Long memberId);
    
    /**
     * 삭제 (물리적 삭제)
     */
    void delete(SavedPaymentMethod savedPaymentMethod);
    
    /**
     * ID로 삭제
     */
    void deleteById(Long id);
    
    /**
     * 특정 결제 타입의 활성 결제수단 개수
     */
    @Query("SELECT COUNT(s) FROM SavedPaymentMethod s " +
           "WHERE s.memberId = :memberId AND s.paymentMethodType = :type AND s.isActive = true")
    long countByMemberIdAndTypeAndActive(@Param("memberId") Long memberId, @Param("type") String type);
    
    /**
     * 페이징된 전체 결제수단 조회
     */
    Page<SavedPaymentMethod> findAll(Pageable pageable);
}