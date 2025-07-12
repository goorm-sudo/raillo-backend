package com.sudo.railo.payment.application.event;

import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public void publishCalculationEvent(String calculationId, String orderId, String userId) {
        PaymentEvent event = PaymentEvent.createCalculationEvent(calculationId, orderId, userId);
        log.info("결제 계산 이벤트 발행: calculationId={}, orderId={}", calculationId, orderId);
        eventPublisher.publishEvent(event);
    }
    
    public void publishExecutionEvent(String paymentId, String orderId, String userId) {
        PaymentEvent event = PaymentEvent.createExecutionEvent(paymentId, orderId, userId);
        log.info("결제 실행 이벤트 발행: paymentId={}, orderId={}", paymentId, orderId);
        eventPublisher.publishEvent(event);
    }
    
    
    /**
     * 결제 상태 변경 이벤트 발행 (Event Sourcing)
     */
    public void publishPaymentStateChanged(Payment payment, 
                                         PaymentExecutionStatus previousStatus,
                                         PaymentExecutionStatus newStatus,
                                         String reason,
                                         String triggeredBy) {
        PaymentStateChangedEvent event = PaymentStateChangedEvent.create(
            payment.getId().toString(),
            payment.getReservationId(),
            previousStatus,
            newStatus,
            reason,
            triggeredBy
        );
        
        log.info("🚀 [PaymentEventPublisher] 결제 상태 변경 이벤트 발행 시작: paymentId={}, reservationId={}, {} → {}, reason={}, triggeredBy={}", 
                payment.getId(), payment.getReservationId(), previousStatus, newStatus, reason, triggeredBy);
        
        eventPublisher.publishEvent(event);
        
        log.info("✅ [PaymentEventPublisher] 이벤트 발행 완료: paymentId={}, reservationId={}", 
                payment.getId(), payment.getReservationId());
    }
    
    /**
     * 결제 상태 변경 이벤트 발행 (메타데이터 포함)
     */
    public void publishPaymentStateChangedWithMetadata(Payment payment, 
                                                      PaymentExecutionStatus previousStatus,
                                                      PaymentExecutionStatus newStatus,
                                                      String reason,
                                                      String triggeredBy,
                                                      Map<String, Object> metadata) {
        PaymentStateChangedEvent event = PaymentStateChangedEvent.create(
            payment.getId().toString(),
            payment.getReservationId(),
            previousStatus,
            newStatus,
            reason,
            triggeredBy
        ).withMetadata(metadata);
        
        log.info("결제 상태 변경 이벤트 발행 (메타데이터 포함): paymentId={}, {} → {}", 
                payment.getId(), previousStatus, newStatus);
        eventPublisher.publishEvent(event);
    }
    
    /**
     * 금액 불일치 알림 이벤트 발행
     */
    public void publishAmountMismatchAlert(String calculationId,
                                         BigDecimal expectedAmount,
                                         BigDecimal actualAmount,
                                         String pgOrderId) {
        AmountMismatchAlertEvent event = AmountMismatchAlertEvent.create(
            calculationId,
            expectedAmount,
            actualAmount,
            pgOrderId,
            null // pgAuthNumber는 옵션
        );
        
        log.warn("⚠️ [PaymentEventPublisher] {}", event.getAlertMessage());
        eventPublisher.publishEvent(event);
    }
}