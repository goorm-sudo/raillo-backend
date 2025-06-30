package com.sudo.railo.payment.application.event;

import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.domain.service.MileageExecutionService;
import com.sudo.railo.payment.exception.InsufficientMileageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

/**
 * 마일리지 관련 이벤트 리스너
 * 결제 완료 이벤트를 수신하여 마일리지 적립/사용을 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MileageEventListener {
    
    private final MileageExecutionService mileageExecutionService;
    private final PaymentRepository paymentRepository;
    
    /**
     * 결제 완료 이벤트 처리 - 마일리지 사용 및 적립
     * 
     * @param event 결제 완료 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event) {
        log.debug("마일리지 이벤트 처리 시작 - 결제ID: {}", event.getPaymentId());
        
        try {
            Payment payment = event.getPayment();
            
            // 1. 마일리지 사용 처리 (회원인 경우)
            if (payment.getMemberId() != null && 
                payment.getMileagePointsUsed() != null && 
                payment.getMileagePointsUsed().compareTo(BigDecimal.ZERO) > 0) {
                
                processMileageUsage(payment);
            }
            
            // 2. 마일리지 적립 처리 (회원인 경우)
            if (payment.getMemberId() != null && 
                payment.getMileageToEarn() != null && 
                payment.getMileageToEarn().compareTo(BigDecimal.ZERO) > 0) {
                
                processMileageEarning(payment);
            }
            
            log.debug("마일리지 이벤트 처리 완료 - 결제ID: {}", event.getPaymentId());
            
        } catch (Exception e) {
            log.error("마일리지 이벤트 처리 중 오류 발생", e);
            // 이벤트 처리 실패는 메인 결제 트랜잭션에 영향주지 않음
            // 별도 보상 작업이나 재시도 로직 필요 시 추가
        }
    }
    
    /**
     * 마일리지 사용 처리
     * 
     * @param payment 결제 정보
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processMileageUsage(Payment payment) {
        log.debug("마일리지 사용 처리 - 회원ID: {}, 사용포인트: {}", 
                payment.getMemberId(), payment.getMileagePointsUsed());
        
        try {
            MileageTransaction usageTransaction = mileageExecutionService.executeUsage(payment);
            
            if (usageTransaction != null) {
                log.debug("마일리지 사용 완료 - 거래ID: {}, 회원ID: {}, 사용포인트: {}", 
                        usageTransaction.getTransactionId(), 
                        payment.getMemberId(), 
                        payment.getMileagePointsUsed());
            }
            
        } catch (InsufficientMileageException e) {
            log.error("마일리지 잔액 부족 - 회원ID: {}, 요청포인트: {}", 
                    payment.getMemberId(), payment.getMileagePointsUsed(), e);
            throw e;
        } catch (Exception e) {
            log.error("마일리지 사용 처리 중 오류 - 회원ID: {}", payment.getMemberId(), e);
            throw e;
        }
    }
    
    /**
     * 마일리지 적립 처리
     * 
     * @param payment 결제 정보
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processMileageEarning(Payment payment) {
        log.info("마일리지 적립 처리 - 회원ID: {}, 적립포인트: {}", 
                payment.getMemberId(), payment.getMileageToEarn());
        
        try {
            MileageTransaction earningTransaction = mileageExecutionService.executeEarning(payment);
            
            if (earningTransaction != null) {
                log.info("마일리지 적립 완료 - 거래ID: {}, 회원ID: {}, 적립포인트: {}", 
                        earningTransaction.getTransactionId(), 
                        payment.getMemberId(), 
                        payment.getMileageToEarn());
            }
            
        } catch (Exception e) {
            log.error("마일리지 적립 처리 중 오류 - 회원ID: {}", payment.getMemberId(), e);
            throw e;
        }
    }
    
    /**
     * 결제 취소 이벤트 처리 - 마일리지 복구
     * 
     * @param event 결제 취소 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    public void handlePaymentCancelledEvent(PaymentCancelledEvent event) {
        log.info("결제 취소 마일리지 처리 시작 - 결제ID: {}", event.getPaymentId());
        
        try {
            Payment payment = event.getPayment();
            
            // 1. 사용한 마일리지 복구
            if (payment.getMemberId() != null && 
                payment.getMileagePointsUsed() != null && 
                payment.getMileagePointsUsed().compareTo(BigDecimal.ZERO) > 0) {
                
                restoreMileageUsage(payment);
            }
            
            // 2. 적립된 마일리지 회수
            if (payment.getMemberId() != null && 
                payment.getMileageToEarn() != null && 
                payment.getMileageToEarn().compareTo(BigDecimal.ZERO) > 0) {
                
                cancelMileageEarning(payment);
            }
            
            log.info("결제 취소 마일리지 처리 완료 - 결제ID: {}", event.getPaymentId());
            
        } catch (Exception e) {
            log.error("결제 취소 마일리지 처리 중 오류 발생 - 결제ID: {}", event.getPaymentId(), e);
        }
    }
    
    /**
     * 마일리지 사용 복구
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreMileageUsage(Payment payment) {
        log.info("마일리지 사용 복구 - 회원ID: {}, 복구포인트: {}", 
                payment.getMemberId(), payment.getMileagePointsUsed());
        
        try {
            MileageTransaction restoreTransaction = mileageExecutionService.restoreUsage(
                    payment.getPaymentId().toString(), 
                    payment.getMemberId(), 
                    payment.getMileagePointsUsed()
            );
            
            log.info("마일리지 사용 복구 완료 - 거래ID: {}", restoreTransaction.getTransactionId());
            
        } catch (Exception e) {
            log.error("마일리지 사용 복구 중 오류 - 회원ID: {}", payment.getMemberId(), e);
            throw e;
        }
    }
    
    /**
     * 마일리지 적립 취소
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelMileageEarning(Payment payment) {
        log.info("마일리지 적립 취소 - 회원ID: {}, 취소포인트: {}", 
                payment.getMemberId(), payment.getMileageToEarn());
        
        try {
            MileageTransaction cancelTransaction = mileageExecutionService.cancelEarning(
                    payment.getPaymentId().toString(), 
                    payment.getMemberId(), 
                    payment.getMileageToEarn()
            );
            
            log.info("마일리지 적립 취소 완료 - 거래ID: {}", cancelTransaction.getTransactionId());
            
        } catch (Exception e) {
            log.error("마일리지 적립 취소 중 오류 - 회원ID: {}", payment.getMemberId(), e);
            throw e;
        }
    }
    
} 