package com.sudo.railo.payment.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudo.railo.payment.application.service.DomainEventOutboxService;
import com.sudo.railo.payment.domain.entity.DomainEventOutbox;
import com.sudo.railo.payment.domain.repository.DomainEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * 결제 상태 변경 이벤트 리스너
 * 
 * Event Sourcing을 위해 모든 결제 상태 변경을 DomainEventOutbox에 저장합니다.
 * 트랜잭션 커밋 후 비동기로 실행되어 성능에 영향을 주지 않습니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentStateEventListener {
    
    private final DomainEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 결제 상태 변경 이벤트 처리
     * 트랜잭션 커밋 후 실행되어 데이터 일관성 보장
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handlePaymentStateChanged(PaymentStateChangedEvent event) {
        try {
            log.info("결제 상태 변경 이벤트 처리 시작 - paymentId: {}, {} → {}", 
                event.getPaymentId(), event.getPreviousStatus(), event.getNewStatus());
            
            // 이벤트 데이터를 JSON으로 변환
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("paymentId", event.getPaymentId());
            eventData.put("reservationId", event.getReservationId());
            eventData.put("previousStatus", event.getPreviousStatus() != null ? event.getPreviousStatus().name() : "null");
            eventData.put("newStatus", event.getNewStatus().name());
            eventData.put("changedAt", event.getChangedAt().toString());
            eventData.put("reason", event.getReason());
            eventData.put("triggeredBy", event.getTriggeredBy());
            
            if (event.getMetadata() != null) {
                eventData.put("metadata", event.getMetadata());
            }
            
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            
            // DomainEventOutbox에 저장
            DomainEventOutbox outboxEvent = DomainEventOutbox.createPaymentStateChangedEvent(
                event.getEventId(),
                event.getPaymentId(),
                eventDataJson
            );
            
            outboxRepository.save(outboxEvent);
            
            // 특별한 상태 변경에 대한 추가 처리
            if (event.isFailureTransition()) {
                handlePaymentFailure(event);
            } else if (event.isSuccessTransition()) {
                handlePaymentSuccess(event);
            } else if (event.isRefundTransition()) {
                handlePaymentRefund(event);
            }
            
            log.info("결제 상태 변경 이벤트 처리 완료 - eventId: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("결제 상태 변경 이벤트 처리 실패 - paymentId: {}", event.getPaymentId(), e);
            // 실패한 이벤트는 재처리를 위해 별도 처리 필요
        }
    }
    
    /**
     * 결제 실패 시 추가 처리
     */
    private void handlePaymentFailure(PaymentStateChangedEvent event) {
        log.warn("결제 실패 처리 - paymentId: {}, reason: {}", 
            event.getPaymentId(), event.getReason());
        // 알림 발송, 모니터링 등 추가 처리
    }
    
    /**
     * 결제 성공 시 추가 처리
     */
    private void handlePaymentSuccess(PaymentStateChangedEvent event) {
        log.info("결제 성공 처리 - paymentId: {}", event.getPaymentId());
        // 통계 업데이트, 리포트 생성 등 추가 처리
    }
    
    /**
     * 결제 환불 시 추가 처리
     */
    private void handlePaymentRefund(PaymentStateChangedEvent event) {
        log.info("결제 환불 처리 - paymentId: {}, reason: {}", 
            event.getPaymentId(), event.getReason());
        // 환불 통계, 원인 분석 등 추가 처리
    }
}