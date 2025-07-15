package com.sudo.railo.payment.application.event;

import com.sudo.railo.payment.application.dto.PaymentResult.MileageExecutionResult;
import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
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
     * 결제 상태 변경 이벤트 처리 - SUCCESS 상태일 때 마일리지 사용 처리
     * 
     * @deprecated 마일리지 사용은 PaymentExecutionService에서 이미 처리하므로 중복 처리 방지를 위해 비활성화
     * 마일리지 적립은 MileageScheduleEventListener에서 처리
     * 
     * @param event 결제 상태 변경 이벤트
     */
    @Deprecated
    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // @Async("taskExecutor")
    public void handlePaymentStateChangedForMileageUsage(PaymentStateChangedEvent event) {
        // PaymentExecutionService.execute()에서 이미 마일리지 사용을 처리하므로
        // 중복 차감을 방지하기 위해 이 메서드는 비활성화
        log.debug("마일리지 사용 처리 스킵 (PaymentExecutionService에서 이미 처리됨) - 결제ID: {}", event.getPaymentId());
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
            MileageExecutionResult result = mileageExecutionService.executeUsage(payment);
            
            if (result != null && result.isSuccess()) {
                log.debug("마일리지 사용 완료 - 거래ID: {}, 회원ID: {}, 사용포인트: {}", 
                        result.getId(), 
                        payment.getMemberId(), 
                        result.getUsedPoints());
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
     * @deprecated 마일리지 적립은 열차 도착 후 MileageScheduleEventListener에서 처리
     * @param payment 결제 정보
     */
    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processMileageEarning(Payment payment) {
        log.debug("마일리지 적립 처리 - 회원ID: {}, 적립포인트: {}", 
                payment.getMemberId(), payment.getMileageToEarn());
        
        try {
            MileageTransaction earningTransaction = mileageExecutionService.executeEarning(payment);
            
            if (earningTransaction != null) {
                log.debug("마일리지 적립 완료 - 거래ID: {}, 회원ID: {}, 적립포인트: {}", 
                        earningTransaction.getId(), 
                        payment.getMemberId(), 
                        payment.getMileageToEarn());
            }
            
        } catch (Exception e) {
            log.error("마일리지 적립 처리 중 오류 - 회원ID: {}", payment.getMemberId(), e);
            throw e;
        }
    }
    
    /**
     * 결제 상태 변경 이벤트 처리 - CANCELLED 상태일 때 마일리지 복구
     * 
     * CANCELLED: 결제 시도 중 실패 (결제 완료 전)
     * REFUNDED: 결제 완료 후 환불 (RefundService에서 처리)
     * 
     * @param event 결제 상태 변경 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    public void handlePaymentStateChangedForCancellation(PaymentStateChangedEvent event) {
        // CANCELLED 상태가 아니면 처리하지 않음
        if (event.getNewStatus() != PaymentExecutionStatus.CANCELLED) {
            return;
        }
        
        log.debug("결제 취소 마일리지 처리 시작 - 결제ID: {}", event.getPaymentId());
        
        try {
            // Payment 엔티티 조회
            Payment payment = paymentRepository.findById(Long.parseLong(event.getPaymentId()))
                    .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + event.getPaymentId()));
            
            // CANCELLED는 결제 완료 전 취소이므로 마일리지 사용 복구만 수행
            // (적립은 아직 되지 않았으므로 취소할 필요 없음)
            if (payment.getMemberId() != null && 
                payment.getMileagePointsUsed() != null && 
                payment.getMileagePointsUsed().compareTo(BigDecimal.ZERO) > 0) {
                
                restoreMileageUsage(payment);
            }
            
            log.debug("결제 취소 마일리지 처리 완료 - 결제ID: {}", event.getPaymentId());
            
        } catch (Exception e) {
            log.error("결제 취소 마일리지 처리 중 오류 발생 - 결제ID: {}", event.getPaymentId(), e);
        }
    }
    
    /**
     * 마일리지 사용 복구
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreMileageUsage(Payment payment) {
        log.debug("마일리지 사용 복구 - 회원ID: {}, 복구포인트: {}", 
                payment.getMemberId(), payment.getMileagePointsUsed());
        
        try {
            MileageTransaction restoreTransaction = mileageExecutionService.restoreUsage(
                    payment.getId().toString(), 
                    payment.getMemberId(), 
                    payment.getMileagePointsUsed()
            );
            
            log.debug("마일리지 사용 복구 완료 - 거래ID: {}", restoreTransaction.getId());
            
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
        log.debug("마일리지 적립 취소 - 회원ID: {}, 취소포인트: {}", 
                payment.getMemberId(), payment.getMileageToEarn());
        
        try {
            MileageTransaction cancelTransaction = mileageExecutionService.cancelEarning(
                    payment.getId().toString(), 
                    payment.getMemberId(), 
                    payment.getMileageToEarn()
            );
            
            log.debug("마일리지 적립 취소 완료 - 거래ID: {}", cancelTransaction.getId());
            
        } catch (Exception e) {
            log.error("마일리지 적립 취소 중 오류 - 회원ID: {}", payment.getMemberId(), e);
            throw e;
        }
    }
    
} 