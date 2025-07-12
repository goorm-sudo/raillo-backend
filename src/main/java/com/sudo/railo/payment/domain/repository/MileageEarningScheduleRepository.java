package com.sudo.railo.payment.domain.repository;

import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 마일리지 적립 스케줄 Repository
 * TrainSchedule과 Payment를 연결하여 실시간 마일리지 적립을 관리
 */
@Repository
public interface MileageEarningScheduleRepository extends JpaRepository<MileageEarningSchedule, Long> {
    
    /**
     * 처리 대기 중인 적립 스케줄 조회 (처리 시간 순)
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.status = 'READY' " +
           "AND mes.scheduledEarningTime <= :currentTime " +
           "ORDER BY mes.scheduledEarningTime ASC")
    List<MileageEarningSchedule> findReadySchedulesForProcessing(
            @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 처리 준비된 스케줄 조회 (제한된 개수)
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.status = 'READY' " +
           "AND mes.scheduledEarningTime <= :currentTime " +
           "ORDER BY mes.scheduledEarningTime ASC " +
           "LIMIT :limit")
    List<MileageEarningSchedule> findReadySchedulesWithLimit(
            @Param("currentTime") LocalDateTime currentTime,
            @Param("limit") int limit);
    
    /**
     * 처리 준비된 스케줄 조회 (비관적 락 사용)
     * FOR UPDATE SKIP LOCKED를 사용하여 다른 트랜잭션이 잠근 행은 건너뜀
     * 동시성 문제를 해결하여 여러 스케줄러가 중복 처리하지 않도록 함
     */
    @Query(value = "SELECT * FROM mileage_earning_schedules mes " +
           "WHERE mes.status = 'READY' " +
           "AND mes.scheduled_earning_time <= :currentTime " +
           "ORDER BY mes.scheduled_earning_time ASC " +
           "LIMIT :limit " +
           "FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<MileageEarningSchedule> findReadySchedulesWithLockAndLimit(
            @Param("currentTime") LocalDateTime currentTime,
            @Param("limit") int limit);
    
    /**
     * 특정 열차 스케줄의 적립 스케줄 조회
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.trainScheduleId = :trainScheduleId " +
           "ORDER BY mes.createdAt DESC")
    List<MileageEarningSchedule> findByTrainScheduleId(@Param("trainScheduleId") Long trainScheduleId);
    
    /**
     * 특정 결제의 적립 스케줄 조회
     */
    Optional<MileageEarningSchedule> findByPaymentId(String paymentId);
    
    /**
     * 회원의 적립 스케줄 조회 (페이징)
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.memberId = :memberId " +
           "ORDER BY mes.createdAt DESC")
    Page<MileageEarningSchedule> findByMemberIdOrderByCreatedAtDesc(
            @Param("memberId") Long memberId, Pageable pageable);
    
    /**
     * 회원의 특정 상태 적립 스케줄 조회
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.memberId = :memberId " +
           "AND mes.status = :status " +
           "ORDER BY mes.scheduledEarningTime DESC")
    List<MileageEarningSchedule> findByMemberIdAndStatus(
            @Param("memberId") Long memberId,
            @Param("status") MileageEarningSchedule.EarningStatus status);
    
    /**
     * 회원의 모든 적립 스케줄 조회 (상태 무관)
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.memberId = :memberId " +
           "ORDER BY mes.scheduledEarningTime DESC")
    List<MileageEarningSchedule> findByMemberId(@Param("memberId") Long memberId);
    
    /**
     * 기본 적립 완료, 지연 보상 대기 중인 스케줄 조회
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.status = 'BASE_COMPLETED' " +
           "AND mes.delayCompensationAmount > 0 " +
           "ORDER BY mes.scheduledEarningTime ASC")
    List<MileageEarningSchedule> findSchedulesAwaitingDelayCompensation();
    
    /**
     * 특정 기간의 적립 예정 스케줄 조회
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.scheduledEarningTime BETWEEN :startTime AND :endTime " +
           "AND mes.status IN ('SCHEDULED', 'READY') " +
           "ORDER BY mes.scheduledEarningTime ASC")
    List<MileageEarningSchedule> findSchedulesByEarningTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 실패한 적립 스케줄 조회 (재처리용)
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.status = 'FAILED' " +
           "ORDER BY mes.updatedAt ASC")
    List<MileageEarningSchedule> findFailedSchedulesForRetry();
    
    /**
     * 특정 열차 스케줄의 상태별 개수 조회
     */
    @Query("SELECT mes.status, COUNT(*) " +
           "FROM MileageEarningSchedule mes " +
           "WHERE mes.trainScheduleId = :trainScheduleId " +
           "GROUP BY mes.status")
    List<Object[]> countByTrainScheduleIdGroupByStatus(@Param("trainScheduleId") Long trainScheduleId);
    
    /**
     * 회원의 총 적립 예정 마일리지 조회
     */
    @Query("SELECT COALESCE(SUM(mes.totalMileageAmount), 0) " +
           "FROM MileageEarningSchedule mes " +
           "WHERE mes.memberId = :memberId " +
           "AND mes.status IN ('SCHEDULED', 'READY', 'BASE_PROCESSING', 'BASE_COMPLETED', 'COMPENSATION_PROCESSING')")
    BigDecimal calculatePendingMileageByMemberId(@Param("memberId") Long memberId);
    
    /**
     * 특정 기간의 적립 완료된 마일리지 통계
     */
    @Query("SELECT new map(" +
           "COUNT(*) as totalSchedules, " +
           "SUM(mes.baseMileageAmount) as totalBaseMileage, " +
           "SUM(mes.delayCompensationAmount) as totalDelayCompensation, " +
           "SUM(mes.totalMileageAmount) as totalMileage, " +
           "COUNT(CASE WHEN mes.delayMinutes > 0 THEN 1 END) as delayedTrainCount, " +
           "AVG(mes.delayMinutes) as averageDelayMinutes) " +
           "FROM MileageEarningSchedule mes " +
           "WHERE mes.status = 'FULLY_COMPLETED' " +
           "AND mes.processedAt BETWEEN :startTime AND :endTime")
    Object getMileageEarningStatistics(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 지연 보상 통계 조회
     */
    @Query("SELECT new map(" +
           "COUNT(CASE WHEN mes.delayMinutes >= 20 AND mes.delayMinutes < 40 THEN 1 END) as delay20to40Count, " +
           "COUNT(CASE WHEN mes.delayMinutes >= 40 AND mes.delayMinutes < 60 THEN 1 END) as delay40to60Count, " +
           "COUNT(CASE WHEN mes.delayMinutes >= 60 THEN 1 END) as delayOver60Count, " +
           "SUM(CASE WHEN mes.delayMinutes >= 20 THEN mes.delayCompensationAmount ELSE 0 END) as totalCompensation) " +
           "FROM MileageEarningSchedule mes " +
           "WHERE mes.processedAt BETWEEN :startTime AND :endTime")
    Object getDelayCompensationStatistics(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 특정 상태의 스케줄 개수 조회
     */
    long countByStatus(MileageEarningSchedule.EarningStatus status);
    
    /**
     * 특정 회원의 완료된 적립 스케줄 조회
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.memberId = :memberId " +
           "AND mes.status = 'FULLY_COMPLETED' " +
           "ORDER BY mes.processedAt DESC")
    Page<MileageEarningSchedule> findCompletedSchedulesByMemberId(
            @Param("memberId") Long memberId, Pageable pageable);
    
    /**
     * 오래된 완료 스케줄 삭제 (정리용)
     */
    @Modifying
    @Query("DELETE FROM MileageEarningSchedule mes " +
           "WHERE mes.status = 'FULLY_COMPLETED' " +
           "AND mes.processedAt < :beforeTime")
    int deleteCompletedSchedulesBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 특정 결제들의 적립 스케줄 조회 (배치 처리용)
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.paymentId IN :paymentIds")
    List<MileageEarningSchedule> findByPaymentIds(@Param("paymentIds") List<String> paymentIds);
    
    /**
     * 스케줄 상태를 READY로 일괄 업데이트
     */
    @Modifying
    @Query("UPDATE MileageEarningSchedule mes " +
           "SET mes.status = 'READY' " +
           "WHERE mes.trainScheduleId = :trainScheduleId " +
           "AND mes.status = 'SCHEDULED'")
    int markSchedulesReadyByTrainSchedule(@Param("trainScheduleId") Long trainScheduleId);
    
    /**
     * 처리 시간이 지났지만 아직 SCHEDULED 상태인 스케줄 조회
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.status = 'SCHEDULED' " +
           "AND mes.scheduledEarningTime <= :currentTime " +
           "ORDER BY mes.scheduledEarningTime ASC")
    List<MileageEarningSchedule> findOverdueScheduledEarnings(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 특정 회원의 월별 마일리지 적립 통계
     */
    @Query("SELECT YEAR(mes.processedAt), MONTH(mes.processedAt), " +
           "SUM(mes.baseMileageAmount), SUM(mes.delayCompensationAmount), SUM(mes.totalMileageAmount) " +
           "FROM MileageEarningSchedule mes " +
           "WHERE mes.memberId = :memberId " +
           "AND mes.status = 'FULLY_COMPLETED' " +
           "AND mes.processedAt >= :fromDate " +
           "GROUP BY YEAR(mes.processedAt), MONTH(mes.processedAt) " +
           "ORDER BY YEAR(mes.processedAt), MONTH(mes.processedAt)")
    List<Object[]> getMonthlyMileageStatisticsByMemberId(
            @Param("memberId") Long memberId,
            @Param("fromDate") LocalDateTime fromDate);
    
    /**
     * 원자적 상태 변경
     * 예상 상태일 때만 새로운 상태로 변경하여 동시성 문제 방지
     * 
     * @return 업데이트된 행 수 (0이면 이미 다른 프로세스가 처리)
     */
    @Modifying
    @Query("UPDATE MileageEarningSchedule mes " +
           "SET mes.status = :newStatus " +
           "WHERE mes.id = :scheduleId " +
           "AND mes.status = :expectedStatus")
    int updateStatusAtomically(@Param("scheduleId") Long scheduleId,
                              @Param("expectedStatus") MileageEarningSchedule.EarningStatus expectedStatus,
                              @Param("newStatus") MileageEarningSchedule.EarningStatus newStatus);
    
    /**
     * 스케줄 처리 완료 시 트랜잭션 정보와 함께 원자적 업데이트
     */
    @Modifying
    @Query("UPDATE MileageEarningSchedule mes " +
           "SET mes.status = :newStatus, " +
           "    mes.baseTransactionId = :transactionId, " +
           "    mes.processedAt = CASE WHEN :isFullyCompleted = true THEN CURRENT_TIMESTAMP ELSE mes.processedAt END " +
           "WHERE mes.id = :scheduleId " +
           "AND mes.status = :expectedStatus")
    int updateWithTransactionAtomically(@Param("scheduleId") Long scheduleId,
                                       @Param("expectedStatus") MileageEarningSchedule.EarningStatus expectedStatus,
                                       @Param("newStatus") MileageEarningSchedule.EarningStatus newStatus,
                                       @Param("transactionId") Long transactionId,
                                       @Param("isFullyCompleted") boolean isFullyCompleted);
    
    /**
     * 도착 시간이 지난 SCHEDULED 상태의 스케줄 조회
     */
    default List<MileageEarningSchedule> findScheduledBeforeTime(LocalDateTime currentTime) {
        return findOverdueScheduledEarnings(currentTime);
    }
    
    /**
     * READY 상태의 모든 스케줄 조회
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.status = 'READY' " +
           "ORDER BY mes.scheduledEarningTime ASC")
    List<MileageEarningSchedule> findReadySchedules();
    
    /**
     * 기본 적립 완료되고 지연 보상이 있는 스케줄 조회
     */
    @Query("SELECT mes FROM MileageEarningSchedule mes " +
           "WHERE mes.status = 'BASE_COMPLETED' " +
           "AND mes.delayCompensationAmount > 0 " +
           "ORDER BY mes.scheduledEarningTime ASC")
    List<MileageEarningSchedule> findBaseCompletedWithCompensation();
} 