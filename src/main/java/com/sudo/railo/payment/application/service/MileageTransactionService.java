package com.sudo.railo.payment.application.service;

import com.sudo.railo.global.redis.annotation.DistributedLock;
import com.sudo.railo.payment.application.dto.response.MileageStatistics;
import com.sudo.railo.payment.application.dto.response.MileageStatisticsResponse;
import com.sudo.railo.payment.application.port.out.LoadMileageTransactionPort;
import com.sudo.railo.payment.application.port.out.SaveMileageTransactionPort;
import com.sudo.railo.payment.application.port.out.SaveMemberInfoPort;
import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.exception.MemberError;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 마일리지 거래 서비스
 * 새로운 EarningType을 지원하는 마일리지 거래 생성 및 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MileageTransactionService {
    
    private final LoadMileageTransactionPort loadMileageTransactionPort;
    private final SaveMileageTransactionPort saveMileageTransactionPort;
    private final SaveMemberInfoPort saveMemberInfoPort;
    private final MemberRepository memberRepository;
    
    /**
     * 기본 마일리지 적립 거래 생성
     */
    @Transactional
    @DistributedLock(key = "#memberId", prefix = "mileage:earn", waitTime = 3)
    public MileageTransaction createBaseEarningTransaction(
            Long memberId,
            String paymentId,
            BigDecimal amount,
            Long trainScheduleId,
            Long earningScheduleId,
            BigDecimal balanceBefore) {
        
        log.info("기본 마일리지 적립 거래 생성 - 회원ID: {}, 결제ID: {}, 금액: {}P", 
                memberId, paymentId, amount);
        
        String description = String.format("기차 이용 기본 마일리지 적립 (1%%) - 결제ID: %s", paymentId);
        
        MileageTransaction transaction = MileageTransaction.createBaseEarningTransaction(
                memberId,
                paymentId,
                trainScheduleId,
                earningScheduleId,
                amount,
                balanceBefore,
                description
        );
        
        transaction = saveMileageTransactionPort.save(transaction);
        
        // 거래 즉시 완료 처리
        // TODO: 열차 도착 발생 시 completed로 변경 필요
        transaction.complete();
        transaction = saveMileageTransactionPort.save(transaction);
        
        // 회원 마일리지 잔액 동기화
        saveMemberInfoPort.addMileage(memberId, amount.longValue());
        
        log.info("기본 마일리지 적립 거래 생성 완료 - 거래ID: {}, 금액: {}P, 상태: {}", 
                transaction.getId(), amount, transaction.getStatus());
        
        return transaction;
    }
    
    /**
     * 지연 보상 마일리지 적립 거래 생성
     */
    @Transactional
    @DistributedLock(key = "#memberId", prefix = "mileage:delay", waitTime = 3)
    public MileageTransaction createDelayCompensationTransaction(
            Long memberId,
            String paymentId,
            BigDecimal compensationAmount,
            Long trainScheduleId,
            Long earningScheduleId,
            int delayMinutes,
            BigDecimal compensationRate,
            BigDecimal balanceBefore) {
        
        log.info("지연 보상 마일리지 적립 거래 생성 - 회원ID: {}, 결제ID: {}, 보상금액: {}P, 지연시간: {}분", 
                memberId, paymentId, compensationAmount, delayMinutes);
        
        String description = String.format("열차 지연 보상 마일리지 (지연 %d분, %.1f%% 보상) - 결제ID: %s", 
                delayMinutes, compensationRate.multiply(new BigDecimal("100")), paymentId);
        
        MileageTransaction transaction = MileageTransaction.createDelayCompensationTransaction(
                memberId,
                paymentId,
                trainScheduleId,
                earningScheduleId,
                compensationAmount,
                balanceBefore,
                delayMinutes,
                compensationRate,
                description
        );
        
        transaction = saveMileageTransactionPort.save(transaction);
        
        // 거래 즉시 완료 처리
        // TODO: 열차 도착 발생 시 completed로 변경 필요
        transaction.complete();
        transaction = saveMileageTransactionPort.save(transaction);
        
        // 회원 마일리지 잔액 동기화
        saveMemberInfoPort.addMileage(memberId, compensationAmount.longValue());
        
        log.info("지연 보상 마일리지 적립 거래 생성 완료 - 거래ID: {}, 보상금액: {}P, 상태: {}", 
                transaction.getId(), compensationAmount, transaction.getStatus());
        
        return transaction;
    }
    
    /**
     * 프로모션 마일리지 적립 거래 생성
     */
    /* // TODO: 프로모션 기능 구현 시 활성화
    @Transactional
    public MileageTransaction createPromotionEarningTransaction(
            Long memberId,
            String paymentId,
            BigDecimal promotionAmount,
            String promotionCode,
            String description) {
        
        log.info("프로모션 마일리지 적립 거래 생성 - 회원ID: {}, 결제ID: {}, 프로모션금액: {}P, 코드: {}", 
                memberId, paymentId, promotionAmount, promotionCode);
        
        MileageTransaction transaction = MileageTransaction.createPromotionEarningTransaction(
                memberId,
                paymentId,
                promotionAmount,
                promotionCode,
                description
        );
        
        transaction = saveMileageTransactionPort.save(transaction);
        
        log.info("프로모션 마일리지 적립 거래 생성 완료 - 거래ID: {}, 프로모션금액: {}P", 
                transaction.getId(), promotionAmount);
        
        return transaction;
    }
    */
    
    /**
     * 수동 조정 마일리지 거래 생성 (관리자용)
     */
    /* // TODO: 관리자 기능 구현 시 활성화
    @Transactional
    public MileageTransaction createManualAdjustmentTransaction(
            Long memberId,
            BigDecimal adjustmentAmount,
            String reason,
            String adminId) {
        
        log.info("수동 조정 마일리지 거래 생성 - 회원ID: {}, 조정금액: {}P, 사유: {}, 관리자ID: {}", 
                memberId, adjustmentAmount, reason, adminId);
        
        MileageTransaction transaction = MileageTransaction.createManualAdjustmentTransaction(
                memberId,
                adjustmentAmount,
                reason,
                adminId
        );
        
        transaction = saveMileageTransactionPort.save(transaction);
        
        log.info("수동 조정 마일리지 거래 생성 완료 - 거래ID: {}, 조정금액: {}P", 
                transaction.getId(), adjustmentAmount);
        
        return transaction;
    }
    */
    
    /**
     * 특정 열차 스케줄의 마일리지 거래 조회
     */
    @Transactional(readOnly = true)
    public List<MileageTransaction> getTransactionsByTrainSchedule(Long trainScheduleId) {
        log.debug("열차 스케줄별 마일리지 거래 조회 - 열차스케줄ID: {}", trainScheduleId);
        
        return loadMileageTransactionPort.findByTrainScheduleId(trainScheduleId);
    }
    
    /**
     * 특정 적립 스케줄의 마일리지 거래 조회
     */
    @Transactional(readOnly = true)
    public List<MileageTransaction> getTransactionsByEarningSchedule(Long earningScheduleId) {
        log.debug("적립 스케줄별 마일리지 거래 조회 - 적립스케줄ID: {}", earningScheduleId);
        
        return loadMileageTransactionPort.findByEarningScheduleId(earningScheduleId);
    }
    
    /**
     * 특정 적립 스케줄의 기본 마일리지 거래 조회
     */
    @Transactional(readOnly = true)
    public Optional<MileageTransaction> getBaseEarningTransaction(Long earningScheduleId) {
        log.debug("기본 마일리지 거래 조회 - 적립스케줄ID: {}", earningScheduleId);
        
        return loadMileageTransactionPort.findBaseEarningByScheduleId(earningScheduleId);
    }
    
    /**
     * 특정 적립 스케줄의 지연 보상 거래 조회
     */
    @Transactional(readOnly = true)
    public Optional<MileageTransaction> getDelayCompensationTransaction(Long earningScheduleId) {
        log.debug("지연 보상 마일리지 거래 조회 - 적립스케줄ID: {}", earningScheduleId);
        
        return loadMileageTransactionPort.findDelayCompensationByScheduleId(earningScheduleId);
    }
    
    /**
     * 회원의 적립 타입별 마일리지 거래 조회
     */
    @Transactional(readOnly = true)
    public List<MileageTransaction> getTransactionsByEarningType(
            Long memberId, MileageTransaction.EarningType earningType) {
        log.debug("적립 타입별 마일리지 거래 조회 - 회원ID: {}, 타입: {}", memberId, earningType);
        
        return loadMileageTransactionPort.findByMemberIdAndEarningType(memberId, earningType);
    }
    
    /**
     * 회원의 지연 보상 총액 계산
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalDelayCompensation(Long memberId) {
        log.debug("회원의 지연 보상 총액 계산 - 회원ID: {}", memberId);
        
        return loadMileageTransactionPort.calculateTotalDelayCompensationByMemberId(memberId);
    }
    
    /**
     * 지연 보상 마일리지 거래 조회 (통계용)
     */
    @Transactional(readOnly = true)
    public List<MileageTransaction> getDelayCompensationTransactions(
            LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("지연 보상 마일리지 거래 조회 - 기간: {} ~ {}", startTime, endTime);
        
        return loadMileageTransactionPort.findDelayCompensationTransactions(startTime, endTime);
    }
    
    /**
     * 적립 타입별 통계 조회
     */
    @Transactional(readOnly = true)
    public List<Object[]> getEarningTypeStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("적립 타입별 통계 조회 - 기간: {} ~ {}", startTime, endTime);
        
        return loadMileageTransactionPort.getEarningTypeStatistics(startTime, endTime);
    }
    
    /**
     * 지연 시간대별 보상 마일리지 통계
     */
    @Transactional(readOnly = true)
    public List<Object[]> getDelayCompensationStatisticsByDelayTime(
            LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("지연 시간대별 보상 마일리지 통계 조회 - 기간: {} ~ {}", startTime, endTime);
        
        return loadMileageTransactionPort.getDelayCompensationStatisticsByDelayTime(startTime, endTime);
    }
    
    /**
     * 회원의 열차 관련 마일리지 적립 내역 조회
     */
    @Transactional(readOnly = true)
    public List<MileageTransaction> getTrainRelatedEarnings(Long memberId) {
        log.debug("회원의 열차 관련 마일리지 적립 내역 조회 - 회원ID: {}", memberId);
        
        return loadMileageTransactionPort.findByMemberIdAndEarningType(
                memberId, MileageTransaction.EarningType.BASE_EARN);
    }
    
    /**
     * 특정 열차 스케줄의 총 지급된 마일리지 계산
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalMileageByTrainSchedule(Long trainScheduleId) {
        log.debug("열차 스케줄별 총 지급 마일리지 계산 - 열차스케줄ID: {}", trainScheduleId);
        
        return loadMileageTransactionPort.calculateTotalMileageByTrainSchedule(trainScheduleId);
    }
    
    /**
     * 특정 결제의 모든 관련 마일리지 거래 조회
     */
    @Transactional(readOnly = true)
    public List<MileageTransaction> getAllMileageTransactionsByPayment(String paymentId) {
        log.debug("결제별 모든 마일리지 거래 조회 - 결제ID: {}", paymentId);
        
        return loadMileageTransactionPort.findAllMileageTransactionsByPaymentId(paymentId);
    }
    
    /**
     * 미처리된 마일리지 거래 조회 (재처리용)
     */
    @Transactional(readOnly = true)
    public List<MileageTransaction> getPendingTransactions(int hours) {
        LocalDateTime beforeTime = LocalDateTime.now().minusHours(hours);
        
        log.debug("미처리된 마일리지 거래 조회 - {}시간 이전", hours);
        
        return loadMileageTransactionPort.findPendingTransactionsBeforeTime(beforeTime);
    }
    
    /**
     * 마일리지 거래 상태 업데이트
     */
    /* // TODO: 범용 상태 업데이트 대신 complete(), cancel() 등 명시적 메서드 사용
    @Transactional
    public void updateTransactionStatus(Long transactionId, 
            MileageTransaction.TransactionStatus newStatus) {
        log.info("마일리지 거래 상태 업데이트 - 거래ID: {}, 새 상태: {}", transactionId, newStatus);
        
        MileageTransaction transaction = mileageTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentException("마일리지 거래를 찾을 수 없습니다 - 거래ID: " + transactionId));
        
        transaction.updateStatus(newStatus);
        mileageTransactionRepository.save(transaction);
        
        log.info("마일리지 거래 상태 업데이트 완료 - 거래ID: {}, 상태: {}", transactionId, newStatus);
    }
    */
    
    /**
     * 마일리지 거래 처리 완료
     */
    @Transactional
    public void completeTransaction(Long transactionId) {
        log.info("마일리지 거래 처리 완료 - 거래ID: {}", transactionId);
        
        MileageTransaction transaction = loadMileageTransactionPort.findById(transactionId)
                .orElseThrow(() -> new PaymentException("마일리지 거래를 찾을 수 없습니다 - 거래ID: " + transactionId));
        
        transaction.complete();
        saveMileageTransactionPort.save(transaction);
        
        log.info("마일리지 거래 처리 완료됨 - 거래ID: {}, 처리시간: {}", 
                transactionId, transaction.getProcessedAt());
    }
    
    /**
     * 마일리지 통계 조회
     */
    @Transactional(readOnly = true)
    public MileageStatisticsResponse getMileageStatistics(UserDetails userDetails, LocalDateTime startDate, LocalDateTime endDate) {
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.debug("마일리지 통계 조회 - 회원ID: {}, 기간: {} ~ {}", memberId, startDate, endDate);
        
        // 기간 내 적립 총액
        BigDecimal totalEarned = loadMileageTransactionPort.calculateTotalEarnedInPeriod(
                memberId, startDate, endDate);
        
        // 기간 내 사용 총액
        BigDecimal totalUsed = loadMileageTransactionPort.calculateTotalUsedInPeriod(
                memberId, startDate, endDate);
        
        // 전체 거래 내역 조회하여 통계 계산
        List<MileageTransaction> allTransactions = loadMileageTransactionPort
                .findByMemberIdOrderByCreatedAtDesc(memberId);
        
        // 적립/사용 건수 계산
        int earnCount = 0;
        int useCount = 0;
        LocalDateTime firstTransactionAt = null;
        LocalDateTime lastEarningAt = null;
        LocalDateTime lastUsageAt = null;
        
        for (MileageTransaction tx : allTransactions) {
            if (firstTransactionAt == null || tx.getCreatedAt().isBefore(firstTransactionAt)) {
                firstTransactionAt = tx.getCreatedAt();
            }
            
            if (tx.getType() == MileageTransaction.TransactionType.EARN) {
                earnCount++;
                if (lastEarningAt == null || tx.getCreatedAt().isAfter(lastEarningAt)) {
                    lastEarningAt = tx.getCreatedAt();
                }
            } else if (tx.getType() == MileageTransaction.TransactionType.USE) {
                useCount++;
                if (lastUsageAt == null || tx.getCreatedAt().isAfter(lastUsageAt)) {
                    lastUsageAt = tx.getCreatedAt();
                }
            }
        }
        
        // MileageStatistics 객체 생성
        MileageStatistics statistics = MileageStatistics.builder()
                .totalTransactions(allTransactions.size())
                .earnTransactionCount(earnCount)
                .useTransactionCount(useCount)
                .totalEarned(totalEarned != null ? totalEarned : BigDecimal.ZERO)
                .totalUsed(totalUsed != null ? totalUsed : BigDecimal.ZERO)
                .averageEarningPerTransaction(earnCount > 0 ? totalEarned.divide(BigDecimal.valueOf(earnCount), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO)
                .averageUsagePerTransaction(useCount > 0 ? totalUsed.divide(BigDecimal.valueOf(useCount), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO)
                .firstTransactionAt(firstTransactionAt)
                .lastEarningAt(lastEarningAt)
                .lastUsageAt(lastUsageAt)
                .build();
        
        return MileageStatisticsResponse.from(memberId, statistics);
    }
    
    /**
     * 마일리지 거래 내역 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<MileageTransaction> getMileageTransactions(UserDetails userDetails, Pageable pageable) {
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.debug("마일리지 거래 내역 조회 - 회원ID: {}, 페이지: {}", memberId, pageable.getPageNumber());
        
        return loadMileageTransactionPort.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }
    
    /**
     * 마일리지 적립 이력 조회
     */
    @Transactional(readOnly = true)
    public List<MileageTransaction> getEarningHistory(UserDetails userDetails, String trainId, 
            LocalDateTime startDate, LocalDateTime endDate) {
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.debug("마일리지 적립 이력 조회 - 회원ID: {}, 기차ID: {}, 기간: {} ~ {}", 
                memberId, trainId, startDate, endDate);
        
        // 기차ID가 지정된 경우
        if (trainId != null && !trainId.isEmpty()) {
            return loadMileageTransactionPort.findEarningHistoryByTrainId(
                    memberId, trainId, startDate, endDate);
        }
        
        // 기간만 지정된 경우
        if (startDate != null && endDate != null) {
            return loadMileageTransactionPort.findEarningHistoryByPeriod(
                    memberId, startDate, endDate);
        }
        
        // 모든 적립 이력
        return loadMileageTransactionPort.findAllEarningHistory(memberId);
    }
    
    /**
     * 회원별 지연 보상 마일리지 거래 조회
     */
    @Transactional(readOnly = true)
    public List<MileageTransaction> getDelayCompensationTransactions(UserDetails userDetails) {
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.debug("회원별 지연 보상 마일리지 거래 조회 - 회원ID: {}", memberId);
        
        return loadMileageTransactionPort.findByMemberIdAndEarningType(
                memberId, MileageTransaction.EarningType.DELAY_COMPENSATION);
    }
} 