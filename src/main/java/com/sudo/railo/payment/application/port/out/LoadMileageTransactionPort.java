package com.sudo.railo.payment.application.port.out;

import com.sudo.railo.payment.domain.entity.MileageTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 마일리지 거래 조회 포트
 * 
 * 애플리케이션 계층에서 마일리지 거래 정보를 조회하기 위한 출력 포트
 * 인프라 계층에서 구현
 */
public interface LoadMileageTransactionPort {
    
    Optional<MileageTransaction> findById(Long id);
    
    Optional<MileageTransaction> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
    
    List<MileageTransaction> findByMemberIdAndType(Long memberId, MileageTransaction.TransactionType type);
    
    Page<MileageTransaction> findByMemberId(Long memberId, Pageable pageable);
    
    BigDecimal calculateBalanceByMemberId(Long memberId);
    
    List<MileageTransaction> findExpiredTransactionsByMemberIdAndDate(Long memberId, LocalDateTime date);
    
    BigDecimal sumEarnedPointsByMemberIdAndDateRange(Long memberId, LocalDateTime startDate, LocalDateTime endDate);
    
    BigDecimal sumUsedPointsByMemberIdAndDateRange(Long memberId, LocalDateTime startDate, LocalDateTime endDate);
    
    Long countTransactionsByMemberIdAndType(Long memberId, MileageTransaction.TransactionType type);
    
    // 추가 메서드들 - 점진적 마이그레이션을 위해 필요
    List<MileageTransaction> findByPaymentId(String paymentId);
    
    List<MileageTransaction> findByPaymentIds(List<String> paymentIds);
    
    List<MileageTransaction> findMileageUsageByPaymentId(String paymentId);
    
    List<MileageTransaction> findByTrainScheduleId(Long trainScheduleId);
    
    List<MileageTransaction> findByEarningScheduleId(Long earningScheduleId);
    
    Optional<MileageTransaction> findBaseEarningByScheduleId(Long earningScheduleId);
    
    Optional<MileageTransaction> findDelayCompensationByScheduleId(Long earningScheduleId);
    
    List<MileageTransaction> findByMemberIdAndEarningType(Long memberId, MileageTransaction.EarningType earningType);
    
    BigDecimal calculateTotalDelayCompensationByMemberId(Long memberId);
    
    List<MileageTransaction> findDelayCompensationTransactions(LocalDateTime startTime, LocalDateTime endTime);
    
    List<Object[]> getEarningTypeStatistics(LocalDateTime startTime, LocalDateTime endTime);
    
    List<Object[]> getDelayCompensationStatisticsByDelayTime(LocalDateTime startTime, LocalDateTime endTime);
    
    Page<MileageTransaction> findTrainRelatedEarningsByMemberId(Long memberId, Pageable pageable);
    
    List<MileageTransaction> findAllEarningHistory(Long memberId);
    
    // 추가 메서드들 - Phase 2.1
    BigDecimal calculateTotalMileageByTrainSchedule(Long trainScheduleId);
    
    List<MileageTransaction> findAllMileageTransactionsByPaymentId(String paymentId);
    
    List<MileageTransaction> findPendingTransactionsBeforeTime(LocalDateTime beforeTime);
    
    BigDecimal calculateTotalEarnedInPeriod(Long memberId, LocalDateTime startDate, LocalDateTime endDate);
    
    BigDecimal calculateTotalUsedInPeriod(Long memberId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<MileageTransaction> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    
    Page<MileageTransaction> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
    
    List<MileageTransaction> findEarningHistoryByTrainId(Long memberId, String trainId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<MileageTransaction> findEarningHistoryByPeriod(Long memberId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<MileageTransaction> findByMemberId(Long memberId);
    
    List<MileageTransaction> findByPaymentIdOrderByCreatedAtDesc(String paymentId);
}