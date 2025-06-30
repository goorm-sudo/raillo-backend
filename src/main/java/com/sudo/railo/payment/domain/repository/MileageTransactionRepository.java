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
    @Query("SELECT COALESCE(SUM(mt.pointsAmount), 0) " +
           "FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.status = 'COMPLETED'")
    BigDecimal calculateCurrentBalance(@Param("memberId") Long memberId);
    
    /**
     * 회원의 활성 마일리지 잔액 계산 (만료되지 않은 것만)
     */
    @Query("SELECT COALESCE(SUM(mt.pointsAmount), 0) " +
           "FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.status = 'COMPLETED' " +
           "AND (mt.expiresAt IS NULL OR mt.expiresAt > :currentTime)")
    BigDecimal calculateActiveBalance(@Param("memberId") Long memberId, 
                                    @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 회원의 최근 거래 내역 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.status = 'COMPLETED' " +
           "ORDER BY mt.createdAt DESC " +
           "LIMIT :limit")
    List<MileageTransaction> findRecentTransactionsByMemberId(@Param("memberId") Long memberId, 
                                                            @Param("limit") Integer limit);
    

    
    /**
     * 만료 예정 마일리지 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.expiresAt BETWEEN :startTime AND :endTime")
    List<MileageTransaction> findExpiringMileage(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 회원의 전체 거래 내역 조회 (페이징)
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "ORDER BY mt.createdAt DESC")
    List<MileageTransaction> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);
    
    /**
     * 기간별 마일리지 거래 통계
     */
    @Query("SELECT new map(" +
           "SUM(CASE WHEN mt.type = 'EARN' THEN mt.pointsAmount ELSE 0 END) as totalEarned, " +
           "SUM(CASE WHEN mt.type = 'USE' THEN ABS(mt.pointsAmount) ELSE 0 END) as totalUsed, " +
           "COUNT(*) as transactionCount) " +
           "FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.createdAt BETWEEN :startDate AND :endDate")
    Object getMileageStatistics(@Param("memberId") Long memberId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * 사용 가능한 마일리지 조회 (FIFO 순서, 만료일 순)
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED' " +
           "AND (mt.expiresAt IS NULL OR mt.expiresAt > :currentTime) " +
           "ORDER BY mt.expiresAt ASC, mt.createdAt ASC")
    List<MileageTransaction> findAvailableMileageForUsage(@Param("memberId") Long memberId,
                                                        @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 특정 결제에 대한 마일리지 사용 내역 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.paymentId = :paymentId " +
           "AND mt.type = 'USE' " +
           "AND mt.status = 'COMPLETED'")
    List<MileageTransaction> findMileageUsageByPaymentId(@Param("paymentId") String paymentId);
    
    /**
     * 특정 결제에 대한 마일리지 적립 내역 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.paymentId = :paymentId " +
           "AND mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED'")
    List<MileageTransaction> findMileageEarningByPaymentId(@Param("paymentId") String paymentId);
} 