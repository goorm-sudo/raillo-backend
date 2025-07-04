package com.sudo.railo.payment.application.event;

import com.sudo.railo.payment.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public void publishCalculationEvent(String calculationId, String orderId, String userId) {
        PaymentEvent event = PaymentEvent.createCalculationEvent(calculationId, orderId, userId);
        log.debug("결제 계산 이벤트 발행: calculationId={}, orderId={}", calculationId, orderId);
        eventPublisher.publishEvent(event);
    }
    
    public void publishExecutionEvent(String paymentId, String orderId, String userId) {
        PaymentEvent event = PaymentEvent.createExecutionEvent(paymentId, orderId, userId);
        log.debug("결제 실행 이벤트 발행: paymentId={}, orderId={}", paymentId, orderId);
        eventPublisher.publishEvent(event);
    }
    
    /**
     * 결제 완료 이벤트 발행 (마일리지 적립 트리거)
     */
    public void publishPaymentCompleted(Payment payment) {
        PaymentCompletedEvent event = PaymentCompletedEvent.from(payment);
        
        log.debug("결제 완료 이벤트 발행: paymentId={}, memberId={}, mileageToEarn={}", 
                payment.getPaymentId(), payment.getMemberId(), payment.getMileageToEarn());
        eventPublisher.publishEvent(event);
    }
    
    /**
     * 결제 취소 이벤트 발행 (마일리지 복구 트리거)
     */
    public void publishPaymentCancelled(Payment payment, String cancelReason) {
        PaymentCancelledEvent event = PaymentCancelledEvent.from(payment, cancelReason);
        
        log.debug("결제 취소 이벤트 발행: paymentId={}, memberId={}, cancelReason={}", 
                payment.getPaymentId(), payment.getMemberId(), cancelReason);
        eventPublisher.publishEvent(event);
    }
} 