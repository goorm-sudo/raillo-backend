package com.sudo.railo.payment.domain.service;

import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.repository.MileageTransactionRepository;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 마일리지 적립/사용 실행 도메인 서비스
 * 실제 마일리지 거래를 처리하고 잔액을 관리
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MileageExecutionService {
    
    private final MileageTransactionRepository mileageTransactionRepository;
    private final MileageService mileageService;
    
    /**
     * 마일리지 적립 실행
     * 결제 완료 후 호출되어 실제 포인트를 적립
     */
    public MileageTransaction executeEarning(Payment payment) {
        if (payment.getMemberId() == null) {
            throw new PaymentValidationException("비회원은 마일리지 적립이 불가능합니다");
        }
        
        if (payment.getMileageToEarn() == null || 
            payment.getMileageToEarn().compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("적립할 마일리지가 없습니다 - 결제ID: {}", payment.getPaymentId());
            return null;
        }
        
        // 1. 현재 잔액 조회
        BigDecimal currentBalance = getCurrentBalance(payment.getMemberId());
        
        // 2. 적립 거래 생성
        MileageTransaction transaction = MileageTransaction.createEarnTransaction(
            payment.getMemberId(),
            payment.getPaymentId().toString(),
            payment.getMileageToEarn(),
            currentBalance,
            String.format("기차표 구매 적립 (결제금액: %s원)", payment.getAmountPaid())
        );
        
        // 3. 거래 완료 처리
        transaction.complete();
        
        // 4. 저장
        MileageTransaction savedTransaction = mileageTransactionRepository.save(transaction);
        
        log.debug("마일리지 적립 완료 - 회원ID: {}, 적립포인트: {}, 적립후잔액: {}", 
                payment.getMemberId(), payment.getMileageToEarn(), transaction.getBalanceAfter());
        
        return savedTransaction;
    }
    
    /**
     * 마일리지 사용 실행
     * 결제 시 호출되어 실제 포인트를 차감
     */
    public MileageTransaction executeUsage(Payment payment) {
        if (payment.getMemberId() == null) {
            throw new PaymentValidationException("비회원은 마일리지 사용이 불가능합니다");
        }
        
        if (payment.getMileagePointsUsed() == null || 
            payment.getMileagePointsUsed().compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("사용할 마일리지가 없습니다 - 결제ID: {}", payment.getPaymentId());
            return null;
        }
        
        // 1. 현재 잔액 조회
        BigDecimal currentBalance = getCurrentBalance(payment.getMemberId());
        
        // 2. 잔액 충분성 검증
        if (currentBalance.compareTo(payment.getMileagePointsUsed()) < 0) {
            throw new PaymentValidationException(
                String.format("마일리지 잔액이 부족합니다. 현재잔액: %s, 사용요청: %s", 
                        currentBalance, payment.getMileagePointsUsed()));
        }
        
        // 3. 사용 거래 생성
        MileageTransaction transaction = MileageTransaction.createUseTransaction(
            payment.getMemberId(),
            payment.getPaymentId().toString(),
            payment.getMileagePointsUsed(),
            currentBalance,
            String.format("기차표 구매 사용 (차감금액: %s원)", payment.getMileageAmountDeducted())
        );
        
        // 4. 거래 완료 처리
        transaction.complete();
        
        // 5. 저장
        MileageTransaction savedTransaction = mileageTransactionRepository.save(transaction);
        
        log.debug("마일리지 사용 완료 - 회원ID: {}, 사용포인트: {}, 사용후잔액: {}", 
                payment.getMemberId(), payment.getMileagePointsUsed(), transaction.getBalanceAfter());
        
        return savedTransaction;
    }
    
    /**
     * 마일리지 사용 취소 (환불 시)
     */
    public MileageTransaction cancelUsage(String paymentId, Long memberId, BigDecimal pointsToRestore) {
        // 1. 현재 잔액 조회
        BigDecimal currentBalance = getCurrentBalance(memberId);
        
        // 2. 복구 거래 생성
        MileageTransaction transaction = MileageTransaction.builder()
                .memberId(memberId)
                .paymentId(paymentId)
                .type(MileageTransaction.TransactionType.REFUND)
                .pointsAmount(pointsToRestore) // 양수로 복구
                .balanceBefore(currentBalance)
                .balanceAfter(currentBalance.add(pointsToRestore))
                .description(String.format("결제 취소로 인한 마일리지 복구 (%s포인트)", pointsToRestore))
                .status(MileageTransaction.TransactionStatus.COMPLETED)
                .processedAt(LocalDateTime.now())
                .build();
        
        // 3. 저장
        MileageTransaction savedTransaction = mileageTransactionRepository.save(transaction);
        
        log.debug("마일리지 사용 취소 완료 - 회원ID: {}, 복구포인트: {}, 복구후잔액: {}", 
                memberId, pointsToRestore, transaction.getBalanceAfter());
        
        return savedTransaction;
    }
    
    /**
     * 마일리지 적립 취소 (환불 시)
     */
    public MileageTransaction cancelEarning(String paymentId, Long memberId, BigDecimal pointsToCancel) {
        // 1. 현재 잔액 조회
        BigDecimal currentBalance = getCurrentBalance(memberId);
        
        // 2. 잔액 충분성 검증
        if (currentBalance.compareTo(pointsToCancel) < 0) {
            throw new PaymentValidationException(
                String.format("적립 취소할 마일리지가 부족합니다. 현재잔액: %s, 취소요청: %s", 
                        currentBalance, pointsToCancel));
        }
        
        // 3. 취소 거래 생성
        MileageTransaction transaction = MileageTransaction.builder()
                .memberId(memberId)
                .paymentId(paymentId)
                .type(MileageTransaction.TransactionType.ADJUST)
                .pointsAmount(pointsToCancel.negate()) // 음수로 차감
                .balanceBefore(currentBalance)
                .balanceAfter(currentBalance.subtract(pointsToCancel))
                .description(String.format("결제 취소로 인한 적립 마일리지 회수 (%s포인트)", pointsToCancel))
                .status(MileageTransaction.TransactionStatus.COMPLETED)
                .processedAt(LocalDateTime.now())
                .build();
        
        // 4. 저장
        MileageTransaction savedTransaction = mileageTransactionRepository.save(transaction);
        
        log.debug("마일리지 적립 취소 완료 - 회원ID: {}, 회수포인트: {}, 회수후잔액: {}", 
                memberId, pointsToCancel, transaction.getBalanceAfter());
        
        return savedTransaction;
    }
    
    /**
     * 마일리지 사용 복구 (결제 취소 시)
     * 
     * @param paymentId 결제 ID
     * @param memberId 회원 ID
     * @param pointsToRestore 복구할 포인트
     * @return 복구 거래 내역
     */
    public MileageTransaction restoreUsage(String paymentId, Long memberId, BigDecimal pointsToRestore) {
        // 1. 현재 잔액 조회
        BigDecimal currentBalance = getCurrentBalance(memberId);
        
        // 2. 복구 거래 생성
        MileageTransaction transaction = MileageTransaction.builder()
                .memberId(memberId)
                .paymentId(paymentId)
                .type(MileageTransaction.TransactionType.REFUND)
                .pointsAmount(pointsToRestore) // 양수로 복구
                .balanceBefore(currentBalance)
                .balanceAfter(currentBalance.add(pointsToRestore))
                .description(String.format("결제 취소로 인한 마일리지 사용 복구 (%s포인트)", pointsToRestore))
                .status(MileageTransaction.TransactionStatus.COMPLETED)
                .processedAt(LocalDateTime.now())
                .build();
        
        // 3. 저장
        MileageTransaction savedTransaction = mileageTransactionRepository.save(transaction);
        
        log.debug("마일리지 사용 복구 완료 - 회원ID: {}, 복구포인트: {}, 복구후잔액: {}", 
                memberId, pointsToRestore, transaction.getBalanceAfter());
        
        return savedTransaction;
    }
    
    /**
     * 회원의 현재 마일리지 잔액 조회
     */
    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance(Long memberId) {
        return mileageTransactionRepository.calculateCurrentBalance(memberId);
    }
    
    /**
     * 회원의 활성 마일리지 잔액 조회 (만료되지 않은 것만)
     */
    @Transactional(readOnly = true)
    public BigDecimal getActiveBalance(Long memberId) {
        return mileageTransactionRepository.calculateActiveBalance(memberId, LocalDateTime.now());
    }
    
    /**
     * 마일리지 만료 처리 (스케줄러에서 호출)
     */
    public void processExpiredMileage() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        
        // 어제부터 오늘까지 만료된 마일리지 조회
        var expiredTransactions = mileageTransactionRepository.findExpiringMileage(oneDayAgo, now);
        
        for (MileageTransaction expiredTransaction : expiredTransactions) {
            // 만료 처리 거래 생성
            BigDecimal currentBalance = getCurrentBalance(expiredTransaction.getMemberId());
            
            MileageTransaction expireTransaction = MileageTransaction.builder()
                    .memberId(expiredTransaction.getMemberId())
                    .type(MileageTransaction.TransactionType.EXPIRE)
                    .pointsAmount(expiredTransaction.getPointsAmount().negate()) // 음수로 차감
                    .balanceBefore(currentBalance)
                    .balanceAfter(currentBalance.subtract(expiredTransaction.getPointsAmount()))
                    .description(String.format("마일리지 만료 (원본적립일: %s)", 
                            expiredTransaction.getCreatedAt().toLocalDate()))
                    .status(MileageTransaction.TransactionStatus.COMPLETED)
                    .processedAt(LocalDateTime.now())
                    .build();
            
            mileageTransactionRepository.save(expireTransaction);
            
            log.debug("마일리지 만료 처리 - 회원ID: {}, 만료포인트: {}", 
                    expiredTransaction.getMemberId(), expiredTransaction.getPointsAmount());
        }
    }
} 