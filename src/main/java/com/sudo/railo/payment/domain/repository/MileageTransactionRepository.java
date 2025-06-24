package com.sudo.railo.payment.domain.repository;

import com.sudo.railo.payment.domain.entity.MileageTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 마일리지 거래 내역 Repository
 */
public interface MileageTransactionRepository extends JpaRepository<MileageTransaction, Long> {
    
    /**
     * 회원의 마일리지 거래 내역 조회 (페이징)
     */
    Page<MileageTransaction> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
    
    /**
     * 회원의 특정 기간 마일리지 거래 내역 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY mt.createdAt DESC")
    Page<MileageTransaction> findByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    /**
     * 특정 결제와 연관된 마일리지 거래 내역 조회
     */
    List<MileageTransaction> findByPaymentIdOrderByCreatedAtDesc(String paymentId);
    
    /**
     * 여러 결제 ID에 대한 마일리지 거래 내역 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.paymentId IN :paymentIds " +
           "ORDER BY mt.createdAt DESC")
    List<MileageTransaction> findByPaymentIds(@Param("paymentIds") List<String> paymentIds);
    
    /**
     * 회원의 현재 마일리지 잔액 계산
     */
    @Query("SELECT COALESCE(SUM(mt.pointsAmount), 0) FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.status = 'COMPLETED'")
    BigDecimal calculateCurrentBalance(@Param("memberId") Long memberId);
    
    /**
     * 회원의 활성 마일리지 잔액 조회 (만료되지 않은 적립분만)
     */
    @Query("SELECT COALESCE(SUM(mt.pointsAmount), 0) FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.status = 'COMPLETED' " +
           "AND (mt.expiresAt IS NULL OR mt.expiresAt > :currentTime)")
    BigDecimal calculateActiveBalance(
            @Param("memberId") Long memberId, 
            @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 회원의 최근 거래 내역 조회 (잔액 확인용)
     */
    Optional<MileageTransaction> findTopByMemberIdAndStatusOrderByCreatedAtDesc(
            Long memberId, 
            MileageTransaction.TransactionStatus status);
    
    /**
     * 만료 예정 마일리지 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.expiresAt BETWEEN :startDate AND :endDate " +
           "ORDER BY mt.expiresAt ASC")
    List<MileageTransaction> findExpiringMileage(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * 회원의 거래 유형별 통계
     */
    @Query("SELECT mt.type, COUNT(mt), SUM(mt.pointsAmount) FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY mt.type")
    List<Object[]> getMileageStatistics(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
} 