package com.sudo.railo.payment.infrastructure.adapter.out.persistence;

import com.sudo.railo.payment.application.port.out.LoadMileageTransactionPort;
import com.sudo.railo.payment.application.port.out.SaveMileageTransactionPort;
import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.infrastructure.persistence.JpaMileageTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 마일리지 거래 영속성 어댑터
 * 
 * 애플리케이션 계층의 포트를 구현하여 실제 데이터베이스 접근을 담당
 * 헥사고날 아키텍처의 아웃바운드 어댑터
 */
@Component
@RequiredArgsConstructor
public class MileageTransactionPersistenceAdapter implements LoadMileageTransactionPort, SaveMileageTransactionPort {
    
    private final JpaMileageTransactionRepository mileageTransactionRepository;
    
    @Override
    public Optional<MileageTransaction> findById(Long id) {
        return mileageTransactionRepository.findById(id);
    }
    
    @Override
    public Optional<MileageTransaction> findTopByMemberIdOrderByCreatedAtDesc(Long memberId) {
        // Repository에는 이 메서드가 없으므로 대체 구현
        List<MileageTransaction> transactions = mileageTransactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        return transactions.isEmpty() ? Optional.empty() : Optional.of(transactions.get(0));
    }
    
    @Override
    public List<MileageTransaction> findByMemberIdAndType(Long memberId, MileageTransaction.TransactionType type) {
        // Repository에는 이 메서드가 없으므로 대체 구현
        return mileageTransactionRepository.findByMemberId(memberId).stream()
            .filter(t -> t.getType().equals(type))
            .toList();
    }
    
    @Override
    public Page<MileageTransaction> findByMemberId(Long memberId, Pageable pageable) {
        return mileageTransactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }
    
    @Override
    public BigDecimal calculateBalanceByMemberId(Long memberId) {
        return mileageTransactionRepository.calculateCurrentBalance(memberId);
    }
    
    @Override
    public List<MileageTransaction> findExpiredTransactionsByMemberIdAndDate(Long memberId, LocalDateTime date) {
        // Repository에는 이 메서드가 없으므로 대체 구현
        return mileageTransactionRepository.findByMemberId(memberId).stream()
            .filter(t -> t.getExpiresAt() != null && t.getExpiresAt().isBefore(date))
            .toList();
    }
    
    @Override
    public BigDecimal sumEarnedPointsByMemberIdAndDateRange(Long memberId, LocalDateTime startDate, LocalDateTime endDate) {
        return mileageTransactionRepository.calculateTotalEarnedInPeriod(memberId, startDate, endDate);
    }
    
    @Override
    public BigDecimal sumUsedPointsByMemberIdAndDateRange(Long memberId, LocalDateTime startDate, LocalDateTime endDate) {
        return mileageTransactionRepository.calculateTotalUsedInPeriod(memberId, startDate, endDate);
    }
    
    @Override
    public Long countTransactionsByMemberIdAndType(Long memberId, MileageTransaction.TransactionType type) {
        // Repository에는 이 메서드가 없으므로 대체 구현
        return (long) mileageTransactionRepository.findByMemberId(memberId).stream()
            .filter(t -> t.getType().equals(type))
            .count();
    }
    
    @Override
    public MileageTransaction save(MileageTransaction transaction) {
        return mileageTransactionRepository.save(transaction);
    }
    
    @Override
    public List<MileageTransaction> findByPaymentId(String paymentId) {
        return mileageTransactionRepository.findByPaymentId(paymentId);
    }
    
    @Override
    public List<MileageTransaction> findByPaymentIds(List<String> paymentIds) {
        return mileageTransactionRepository.findByPaymentIds(paymentIds);
    }
    
    @Override
    public List<MileageTransaction> findMileageUsageByPaymentId(String paymentId) {
        return mileageTransactionRepository.findMileageUsageByPaymentId(paymentId);
    }
    
    @Override
    public List<MileageTransaction> findByTrainScheduleId(Long trainScheduleId) {
        return mileageTransactionRepository.findByTrainScheduleId(trainScheduleId);
    }
    
    @Override
    public List<MileageTransaction> findByEarningScheduleId(Long earningScheduleId) {
        return mileageTransactionRepository.findByEarningScheduleId(earningScheduleId);
    }
    
    @Override
    public Optional<MileageTransaction> findBaseEarningByScheduleId(Long earningScheduleId) {
        return mileageTransactionRepository.findBaseEarningByScheduleId(earningScheduleId);
    }
    
    @Override
    public Optional<MileageTransaction> findDelayCompensationByScheduleId(Long earningScheduleId) {
        return mileageTransactionRepository.findDelayCompensationByScheduleId(earningScheduleId);
    }
    
    @Override
    public List<MileageTransaction> findByMemberIdAndEarningType(Long memberId, MileageTransaction.EarningType earningType) {
        return mileageTransactionRepository.findByMemberIdAndEarningType(memberId, earningType);
    }
    
    @Override
    public BigDecimal calculateTotalDelayCompensationByMemberId(Long memberId) {
        return mileageTransactionRepository.calculateTotalDelayCompensationByMemberId(memberId);
    }
    
    @Override
    public List<MileageTransaction> findDelayCompensationTransactions(LocalDateTime startTime, LocalDateTime endTime) {
        return mileageTransactionRepository.findDelayCompensationTransactions(startTime, endTime);
    }
    
    @Override
    public List<Object[]> getEarningTypeStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return mileageTransactionRepository.getEarningTypeStatistics(startTime, endTime);
    }
    
    @Override
    public List<Object[]> getDelayCompensationStatisticsByDelayTime(LocalDateTime startTime, LocalDateTime endTime) {
        return mileageTransactionRepository.getDelayCompensationStatisticsByDelayTime(startTime, endTime);
    }
    
    @Override
    public Page<MileageTransaction> findTrainRelatedEarningsByMemberId(Long memberId, Pageable pageable) {
        return mileageTransactionRepository.findTrainRelatedEarningsByMemberId(memberId, pageable);
    }
    
    @Override
    public List<MileageTransaction> findAllEarningHistory(Long memberId) {
        return mileageTransactionRepository.findAllEarningHistory(memberId);
    }
    
    @Override
    public BigDecimal calculateTotalMileageByTrainSchedule(Long trainScheduleId) {
        return mileageTransactionRepository.calculateTotalMileageByTrainSchedule(trainScheduleId);
    }
    
    @Override
    public List<MileageTransaction> findAllMileageTransactionsByPaymentId(String paymentId) {
        return mileageTransactionRepository.findAllMileageTransactionsByPaymentId(paymentId);
    }
    
    @Override
    public List<MileageTransaction> findPendingTransactionsBeforeTime(LocalDateTime beforeTime) {
        return mileageTransactionRepository.findPendingTransactionsBeforeTime(beforeTime);
    }
    
    @Override
    public BigDecimal calculateTotalEarnedInPeriod(Long memberId, LocalDateTime startDate, LocalDateTime endDate) {
        return mileageTransactionRepository.calculateTotalEarnedInPeriod(memberId, startDate, endDate);
    }
    
    @Override
    public BigDecimal calculateTotalUsedInPeriod(Long memberId, LocalDateTime startDate, LocalDateTime endDate) {
        return mileageTransactionRepository.calculateTotalUsedInPeriod(memberId, startDate, endDate);
    }
    
    @Override
    public List<MileageTransaction> findByMemberIdOrderByCreatedAtDesc(Long memberId) {
        return mileageTransactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }
    
    @Override
    public Page<MileageTransaction> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable) {
        return mileageTransactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }
    
    @Override
    public List<MileageTransaction> findEarningHistoryByTrainId(Long memberId, String trainId, LocalDateTime startDate, LocalDateTime endDate) {
        return mileageTransactionRepository.findEarningHistoryByTrainId(memberId, trainId, startDate, endDate);
    }
    
    @Override
    public List<MileageTransaction> findEarningHistoryByPeriod(Long memberId, LocalDateTime startDate, LocalDateTime endDate) {
        return mileageTransactionRepository.findEarningHistoryByPeriod(memberId, startDate, endDate);
    }
    
    @Override
    public List<MileageTransaction> findByMemberId(Long memberId) {
        return mileageTransactionRepository.findByMemberId(memberId);
    }
    
    @Override
    public List<MileageTransaction> findByPaymentIdOrderByCreatedAtDesc(String paymentId) {
        return mileageTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }
}