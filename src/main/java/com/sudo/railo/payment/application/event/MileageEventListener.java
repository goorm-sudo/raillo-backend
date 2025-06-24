package com.sudo.railo.payment.application.event;

import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.domain.service.MileageExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마일리지 관련 이벤트 리스너
 * 결제 완료 시 마일리지 적립을 자동으로 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MileageEventListener {
    
    private final MileageExecutionService mileageExecutionService;
    private final PaymentRepository paymentRepository;
    
    /**
     * 결제 완료 이벤트 처리
     * 비동기로 마일리지 적립 실행
     */
    @EventListener
    @Async
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("결제 완료 이벤트 수신 - 결제ID: {}, 회원ID: {}, 적립예정: {}포인트", 
                    event.getPaymentId(), event.getMemberId(), event.getMileageToEarn());
            
            // 1. 결제 정보 조회
            Payment payment = paymentRepository.findById(Long.parseLong(event.getPaymentId()))
                    .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다: " + event.getPaymentId()));
            
            // 2. 회원 결제인지 확인
            if (payment.getMemberId() == null) {
                log.info("비회원 결제로 마일리지 적립 건너뜀 - 결제ID: {}", event.getPaymentId());
                return;
            }
            
            // 3. 마일리지 적립 실행
            var transaction = mileageExecutionService.executeEarning(payment);
            
            if (transaction != null) {
                log.info("마일리지 적립 완료 - 결제ID: {}, 회원ID: {}, 적립포인트: {}, 적립후잔액: {}", 
                        event.getPaymentId(), event.getMemberId(), 
                        transaction.getPointsAmount(), transaction.getBalanceAfter());
            } else {
                log.info("적립할 마일리지가 없어 건너뜀 - 결제ID: {}", event.getPaymentId());
            }
            
        } catch (Exception e) {
            log.error("마일리지 적립 처리 중 오류 발생 - 결제ID: {}, 오류: {}", 
                    event.getPaymentId(), e.getMessage(), e);
            
            // 마일리지 적립 실패는 결제 자체를 실패시키지 않음
            // 별도 알림이나 재처리 로직 필요시 여기에 추가
        }
    }
    
    /**
     * 결제 취소 이벤트 처리 (향후 확장용)
     */
    @EventListener
    @Async
    @Transactional
    public void handlePaymentCancelled(PaymentCancelledEvent event) {
        try {
            log.info("결제 취소 이벤트 수신 - 결제ID: {}, 회원ID: {}", 
                    event.getPaymentId(), event.getMemberId());
            
            // 마일리지 사용 복구 및 적립 회수 로직
            if (event.getMileagePointsUsed() != null && 
                event.getMileagePointsUsed().compareTo(java.math.BigDecimal.ZERO) > 0) {
                
                // 사용한 마일리지 복구
                mileageExecutionService.cancelUsage(
                    event.getPaymentId(), 
                    event.getMemberId(), 
                    event.getMileagePointsUsed()
                );
            }
            
            if (event.getMileageToEarn() != null && 
                event.getMileageToEarn().compareTo(java.math.BigDecimal.ZERO) > 0) {
                
                // 적립한 마일리지 회수
                mileageExecutionService.cancelEarning(
                    event.getPaymentId(), 
                    event.getMemberId(), 
                    event.getMileageToEarn()
                );
            }
            
            log.info("결제 취소에 따른 마일리지 처리 완료 - 결제ID: {}", event.getPaymentId());
            
        } catch (Exception e) {
            log.error("결제 취소 마일리지 처리 중 오류 발생 - 결제ID: {}, 오류: {}", 
                    event.getPaymentId(), e.getMessage(), e);
        }
    }
    
    /**
     * 결제 취소 이벤트 (향후 확장용)
     */
    public static class PaymentCancelledEvent {
        private final String paymentId;
        private final Long memberId;
        private final java.math.BigDecimal mileagePointsUsed;
        private final java.math.BigDecimal mileageToEarn;
        
        public PaymentCancelledEvent(String paymentId, Long memberId, 
                                   java.math.BigDecimal mileagePointsUsed, 
                                   java.math.BigDecimal mileageToEarn) {
            this.paymentId = paymentId;
            this.memberId = memberId;
            this.mileagePointsUsed = mileagePointsUsed;
            this.mileageToEarn = mileageToEarn;
        }
        
        public String getPaymentId() { return paymentId; }
        public Long getMemberId() { return memberId; }
        public java.math.BigDecimal getMileagePointsUsed() { return mileagePointsUsed; }
        public java.math.BigDecimal getMileageToEarn() { return mileageToEarn; }
    }
} 