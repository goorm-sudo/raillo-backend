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
     * 회원의 모든 마일리지 거래 내역 조회 (테스트 호환용)
     */
    List<MileageTransaction> findByMemberId(Long memberId);
    
    /**
     * 특정 결제와 연관된 마일리지 거래 내역 조회
     */
    List<MileageTransaction> findByPaymentIdOrderByCreatedAtDesc(String paymentId);
    
    /**
     * 특정 결제와 연관된 마일리지 거래 내역 조회 (테스트 호환용)
     */
    List<MileageTransaction> findByPaymentId(String paymentId);
    
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

    // 🆕 새로운 마일리지 시스템을 위한 메서드들
    
    /**
     * 특정 열차 스케줄과 관련된 마일리지 거래 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.trainScheduleId = :trainScheduleId " +
           "ORDER BY mt.createdAt DESC")
    List<MileageTransaction> findByTrainScheduleId(@Param("trainScheduleId") Long trainScheduleId);
    
    /**
     * 특정 적립 스케줄과 관련된 마일리지 거래 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.earningScheduleId = :earningScheduleId " +
           "ORDER BY mt.createdAt ASC")
    List<MileageTransaction> findByEarningScheduleId(@Param("earningScheduleId") Long earningScheduleId);
    
    /**
     * 특정 적립 스케줄의 기본 마일리지 거래 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.earningScheduleId = :earningScheduleId " +
           "AND mt.earningType = 'BASE_EARN' " +
           "AND mt.status = 'COMPLETED'")
    Optional<MileageTransaction> findBaseEarningByScheduleId(@Param("earningScheduleId") Long earningScheduleId);
    
    /**
     * 특정 적립 스케줄의 지연 보상 거래 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.earningScheduleId = :earningScheduleId " +
           "AND mt.earningType = 'DELAY_COMPENSATION' " +
           "AND mt.status = 'COMPLETED'")
    Optional<MileageTransaction> findDelayCompensationByScheduleId(@Param("earningScheduleId") Long earningScheduleId);
    
    /**
     * 회원의 적립 타입별 마일리지 거래 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'EARN' " +
           "AND mt.earningType = :earningType " +
           "AND mt.status = 'COMPLETED' " +
           "ORDER BY mt.createdAt DESC")
    List<MileageTransaction> findByMemberIdAndEarningType(
            @Param("memberId") Long memberId,
            @Param("earningType") MileageTransaction.EarningType earningType);
    
    /**
     * 지연 보상 마일리지 거래 조회 (통계용)
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.earningType = 'DELAY_COMPENSATION' " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY mt.createdAt DESC")
    List<MileageTransaction> findDelayCompensationTransactions(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 회원의 지연 보상 총액 계산
     */
    @Query("SELECT COALESCE(SUM(mt.pointsAmount), 0) " +
           "FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.earningType = 'DELAY_COMPENSATION' " +
           "AND mt.status = 'COMPLETED'")
    BigDecimal calculateTotalDelayCompensationByMemberId(@Param("memberId") Long memberId);
    
    /**
     * 특정 기간의 적립 타입별 통계
     */
    @Query("SELECT mt.earningType, " +
           "COUNT(*) as transactionCount, " +
           "SUM(mt.pointsAmount) as totalAmount, " +
           "AVG(mt.delayMinutes) as averageDelayMinutes " +
           "FROM MileageTransaction mt " +
           "WHERE mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY mt.earningType")
    List<Object[]> getEarningTypeStatistics(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 특정 회원의 열차별 마일리지 적립 내역
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.trainScheduleId IS NOT NULL " +
           "AND mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED' " +
           "ORDER BY mt.createdAt DESC")
    Page<MileageTransaction> findTrainRelatedEarningsByMemberId(
            @Param("memberId") Long memberId, Pageable pageable);
    
    /**
     * 지연 시간대별 보상 마일리지 통계
     */
    @Query("SELECT " +
           "CASE " +
           "  WHEN mt.delayMinutes >= 20 AND mt.delayMinutes < 40 THEN '20-40min' " +
           "  WHEN mt.delayMinutes >= 40 AND mt.delayMinutes < 60 THEN '40-60min' " +
           "  WHEN mt.delayMinutes >= 60 THEN '60min+' " +
           "  ELSE 'no_delay' " +
           "END as delayRange, " +
           "COUNT(*) as transactionCount, " +
           "SUM(mt.pointsAmount) as totalCompensation " +
           "FROM MileageTransaction mt " +
           "WHERE mt.earningType = 'DELAY_COMPENSATION' " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY delayRange")
    List<Object[]> getDelayCompensationStatisticsByDelayTime(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 특정 결제의 모든 관련 마일리지 거래 조회 (사용 + 적립)
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.paymentId = :paymentId " +
           "ORDER BY " +
           "CASE WHEN mt.type = 'USE' THEN 1 " +
           "     WHEN mt.earningType = 'BASE_EARN' THEN 2 " +
           "     WHEN mt.earningType = 'DELAY_COMPENSATION' THEN 3 " +
           "     ELSE 4 END, " +
           "mt.createdAt ASC")
    List<MileageTransaction> findAllMileageTransactionsByPaymentId(@Param("paymentId") String paymentId);
    
    /**
     * 회원의 마일리지 적립 내역 조회 (기본 + 지연보상)
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'EARN' " +
           "AND mt.earningType IN ('BASE_EARN', 'DELAY_COMPENSATION') " +
           "AND mt.status = 'COMPLETED' " +
           "ORDER BY mt.createdAt DESC")
    Page<MileageTransaction> findTrainEarningsByMemberId(
            @Param("memberId") Long memberId, Pageable pageable);
    
    /**
     * 특정 열차 스케줄의 총 지급된 마일리지 계산
     */
    @Query("SELECT COALESCE(SUM(mt.pointsAmount), 0) " +
           "FROM MileageTransaction mt " +
           "WHERE mt.trainScheduleId = :trainScheduleId " +
           "AND mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED'")
    BigDecimal calculateTotalMileageByTrainSchedule(@Param("trainScheduleId") Long trainScheduleId);
    
    /**
     * 미처리된 마일리지 거래 조회 (재처리용)
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.status = 'PENDING' " +
           "AND mt.createdAt < :beforeTime " +
           "ORDER BY mt.createdAt ASC")
    List<MileageTransaction> findPendingTransactionsBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 회원의 특정 기간 마일리지 적립 총액 계산
     */
    @Query("SELECT COALESCE(SUM(mt.pointsAmount), 0) " +
           "FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalEarnedInPeriod(@Param("memberId") Long memberId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * 회원의 특정 기간 마일리지 사용 총액 계산
     */
    @Query("SELECT COALESCE(SUM(ABS(mt.pointsAmount)), 0) " +
           "FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'USE' " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalUsedInPeriod(@Param("memberId") Long memberId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
    
    /**
     * 회원의 특정 기간 만료된 마일리지 총액 계산
     */
    @Query("SELECT COALESCE(SUM(mt.pointsAmount), 0) " +
           "FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'EXPIRE' " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalExpiredInPeriod(@Param("memberId") Long memberId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * 회원의 특정 기간 타입별 적립 내역
     */
    @Query("SELECT mt.earningType, COUNT(*), SUM(mt.pointsAmount) " +
           "FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY mt.earningType")
    List<Object[]> getEarningByTypeInPeriod(@Param("memberId") Long memberId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * 회원의 특정 기차별 마일리지 적립 이력 조회
     * Native Query를 사용하여 train_schedule 테이블과 조인
     */
    @Query(value = "SELECT mt.* FROM mileage_transactions mt " +
           "JOIN train_schedule ts ON mt.train_schedule_id = ts.id " +
           "WHERE mt.member_id = :memberId " +
           "AND ts.train_no = :trainId " +
           "AND mt.transaction_type = 'EARN' " +
           "AND mt.status = 'COMPLETED' " +
           "AND (:startDate IS NULL OR mt.created_at >= :startDate) " +
           "AND (:endDate IS NULL OR mt.created_at <= :endDate) " +
           "ORDER BY mt.created_at DESC",
           nativeQuery = true)
    List<MileageTransaction> findEarningHistoryByTrainId(@Param("memberId") Long memberId,
                                                        @Param("trainId") String trainId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    /**
     * 회원의 특정 기간 마일리지 적립 이력 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED' " +
           "AND mt.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY mt.createdAt DESC")
    List<MileageTransaction> findEarningHistoryByPeriod(@Param("memberId") Long memberId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);
    
    /**
     * 회원의 모든 마일리지 적립 이력 조회
     */
    @Query("SELECT mt FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED' " +
           "ORDER BY mt.createdAt DESC")
    List<MileageTransaction> findAllEarningHistory(@Param("memberId") Long memberId);
} 