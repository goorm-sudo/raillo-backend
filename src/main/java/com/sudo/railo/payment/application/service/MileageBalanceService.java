package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.response.MileageBalanceInfo;
import com.sudo.railo.payment.application.dto.response.MileageStatistics;
import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.domain.repository.MileageTransactionRepository;
import com.sudo.railo.payment.domain.service.MileageExecutionService;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 마일리지 잔액 조회 애플리케이션 서비스
 * 회원의 마일리지 잔액, 거래 내역 등을 조회하는 기능 제공
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MileageBalanceService {
    
    private final MileageTransactionRepository mileageTransactionRepository;
    private final MileageExecutionService mileageExecutionService;
    
    /**
     * 회원의 마일리지 잔액 정보 조회
     */
    public MileageBalanceInfo getMileageBalance(Long memberId) {
        log.debug("마일리지 잔액 조회 - 회원ID: {}", memberId);
        
        // 현재 총 잔액을 조회합니다.
        BigDecimal currentBalance = mileageExecutionService.getCurrentBalance(memberId);
        
        // 활성 잔액을 조회합니다. 만료되지 않은 마일리지만 포함합니다.
        BigDecimal activeBalance = mileageExecutionService.getActiveBalance(memberId);
        
        // 최근 거래 내역을 조회합니다. 최근 10건의 거래를 조회합니다.
        List<MileageTransaction> recentTransactions = 
                mileageTransactionRepository.findRecentTransactionsByMemberId(memberId, 10);
        
        // 마지막 거래 시간을 조회합니다.
        LocalDateTime lastTransactionAt = recentTransactions.stream()
                .map(MileageTransaction::getProcessedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        
        // 통계 정보를 계산합니다.
        MileageStatistics statistics = calculateStatistics(memberId);
        
        // 만료 예정 마일리지를 조회합니다.
        BigDecimal expiringMileage = calculateExpiringMileage(memberId);
        
        log.debug("마일리지 잔액 조회 완료 - 회원ID: {}, 현재잔액: {}, 활성잔액: {}, 만료예정: {}", 
                memberId, currentBalance, activeBalance, expiringMileage);
        
        return MileageBalanceInfo.builder()
                .memberId(memberId)
                .currentBalance(currentBalance)
                .activeBalance(activeBalance)
                .expiringMileage(expiringMileage)
                .lastTransactionAt(lastTransactionAt)
                .statistics(statistics)
                .recentTransactions(recentTransactions.stream()
                        .map(this::convertToTransactionSummary)
                        .toList())
                .build();
    }
    
    /**
     * 회원의 마일리지 통계 정보 계산
     */
    private MileageStatistics calculateStatistics(Long memberId) {
        log.debug("마일리지 통계 계산 - 회원ID: {}", memberId);
        
        // 전체 거래 내역을 조회합니다.
        List<MileageTransaction> allTransactions = 
                mileageTransactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        
        if (allTransactions.isEmpty()) {
            return MileageStatistics.builder()
                    .totalEarned(BigDecimal.ZERO)
                    .totalUsed(BigDecimal.ZERO)
                    .totalTransactions(0)
                    .averageEarningPerTransaction(BigDecimal.ZERO)
                    .averageUsagePerTransaction(BigDecimal.ZERO)
                    .firstTransactionAt(null)
                    .build();
        }
        
        // 적립 통계를 계산합니다.
        List<MileageTransaction> earnTransactions = allTransactions.stream()
                .filter(t -> t.getType() == MileageTransaction.TransactionType.EARN)
                .filter(t -> t.getStatus() == MileageTransaction.TransactionStatus.COMPLETED)
                .toList();
        
        BigDecimal totalEarned = earnTransactions.stream()
                .map(MileageTransaction::getPointsAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 사용 통계를 계산합니다.
        List<MileageTransaction> useTransactions = allTransactions.stream()
                .filter(t -> t.getType() == MileageTransaction.TransactionType.USE)
                .filter(t -> t.getStatus() == MileageTransaction.TransactionStatus.COMPLETED)
                .toList();
        
        BigDecimal totalUsed = useTransactions.stream()
                .map(MileageTransaction::getPointsAmount)
                .map(BigDecimal::abs) // 사용은 음수로 저장되므로 절댓값
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 평균을 계산합니다.
        BigDecimal averageEarning = earnTransactions.isEmpty() 
                ? BigDecimal.ZERO 
                : totalEarned.divide(BigDecimal.valueOf(earnTransactions.size()), 0, BigDecimal.ROUND_HALF_UP);
        
        BigDecimal averageUsage = useTransactions.isEmpty() 
                ? BigDecimal.ZERO 
                : totalUsed.divide(BigDecimal.valueOf(useTransactions.size()), 0, BigDecimal.ROUND_HALF_UP);
        
        // 첫 거래 시간을 조회합니다.
        LocalDateTime firstTransactionAt = allTransactions.stream()
                .map(MileageTransaction::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        
        return MileageStatistics.builder()
                .totalEarned(totalEarned)
                .totalUsed(totalUsed)
                .totalTransactions(allTransactions.size())
                .earnTransactionCount(earnTransactions.size())
                .useTransactionCount(useTransactions.size())
                .averageEarningPerTransaction(averageEarning)
                .averageUsagePerTransaction(averageUsage)
                .firstTransactionAt(firstTransactionAt)
                .build();
    }
    
    /**
     * 30일 이내 만료 예정 마일리지 계산
     */
    private BigDecimal calculateExpiringMileage(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysLater = now.plusDays(30);
        
        List<MileageTransaction> expiringTransactions = 
                mileageTransactionRepository.findExpiringMileage(now, thirtyDaysLater);
        
        return expiringTransactions.stream()
                .filter(t -> t.getMemberId().equals(memberId))
                .filter(t -> t.getType() == MileageTransaction.TransactionType.EARN)
                .filter(t -> t.getStatus() == MileageTransaction.TransactionStatus.COMPLETED)
                .map(MileageTransaction::getPointsAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 마일리지 거래를 요약 정보로 변환
     */
    private MileageBalanceInfo.TransactionSummary convertToTransactionSummary(MileageTransaction transaction) {
        return MileageBalanceInfo.TransactionSummary.builder()
                .transactionId(transaction.getTransactionId())
                .type(transaction.getType().getDescription())
                .amount(transaction.getPointsAmount())
                .description(transaction.getDescription())
                .processedAt(transaction.getProcessedAt())
                .status(transaction.getStatus().getDescription())
                .build();
    }
    
    /**
     * 회원의 사용 가능한 마일리지 내역 조회 (FIFO 순서)
     */
    public List<MileageTransaction> getAvailableMileageForUsage(Long memberId) {
        log.debug("사용 가능한 마일리지 조회 - 회원ID: {}", memberId);
        
        return mileageTransactionRepository.findAvailableMileageForUsage(memberId, LocalDateTime.now());
    }
    
    /**
     * 특정 기간의 마일리지 거래 통계
     */
    public MileageStatistics getMileageStatisticsByPeriod(Long memberId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("기간별 마일리지 통계 조회 - 회원ID: {}, 기간: {} ~ {}", memberId, startDate, endDate);
        
        Object statisticsObj = mileageTransactionRepository.getMileageStatistics(memberId, startDate, endDate);
        
        if (statisticsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (Map<String, Object>) statisticsObj;
            
            BigDecimal totalEarned = (BigDecimal) stats.get("totalEarned");
            BigDecimal totalUsed = (BigDecimal) stats.get("totalUsed");
            Long transactionCount = (Long) stats.get("transactionCount");
            
            return MileageStatistics.builder()
                    .totalEarned(totalEarned != null ? totalEarned : BigDecimal.ZERO)
                    .totalUsed(totalUsed != null ? totalUsed : BigDecimal.ZERO)
                    .totalTransactions(transactionCount != null ? transactionCount.intValue() : 0)
                    .build();
        }
        
        return MileageStatistics.builder()
                .totalEarned(BigDecimal.ZERO)
                .totalUsed(BigDecimal.ZERO)
                .totalTransactions(0)
                .build();
    }
} 