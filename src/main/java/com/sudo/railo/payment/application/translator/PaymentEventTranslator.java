package com.sudo.railo.payment.application.translator;

import com.sudo.railo.booking.application.event.BookingPaymentCancelledEvent;
import com.sudo.railo.booking.application.event.BookingPaymentCompletedEvent;
import com.sudo.railo.booking.application.event.BookingPaymentFailedEvent;
import com.sudo.railo.booking.application.event.BookingPaymentRefundedEvent;
import com.sudo.railo.payment.application.event.PaymentStateChangedEvent;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 결제 이벤트를 예약 도메인 이벤트로 변환하는 트랜슬레이터
 * 
 * Event Translator 패턴을 구현하여 Payment 도메인의 이벤트를
 * Booking 도메인이 이해할 수 있는 이벤트로 변환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventTranslator {
    
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 결제 상태 변경 이벤트를 처리하고 적절한 예약 이벤트로 변환
     */
    @EventListener
    public void handlePaymentStateChanged(PaymentStateChangedEvent event) {
        try {
            log.info("🎯 [PaymentEventTranslator] 이벤트 수신 - paymentId: {}, reservationId: {}, {} → {}", 
                    event.getPaymentId(), event.getReservationId(), event.getPreviousStatus(), event.getNewStatus());
            
            // null 상태 체크
            if (event.getNewStatus() == null) {
                log.warn("❌ [PaymentEventTranslator] 결제 상태가 null입니다 - paymentId: {}", event.getPaymentId());
                return;
            }
            
            // 새로운 상태에 따라 적절한 예약 이벤트 발행
            switch (event.getNewStatus()) {
                case SUCCESS:
                    log.info("✅ [PaymentEventTranslator] SUCCESS 상태 감지 - 결제 완료 이벤트 발행 시작");
                    publishCompletedEvent(event);
                    break;
                case FAILED:
                    log.info("❌ [PaymentEventTranslator] FAILED 상태 감지 - 결제 실패 이벤트 발행 시작");
                    publishFailedEvent(event);
                    break;
                case CANCELLED:
                    log.info("🚫 [PaymentEventTranslator] CANCELLED 상태 감지 - 결제 취소 이벤트 발행 시작");
                    publishCancelledEvent(event);
                    break;
                case REFUNDED:
                    log.info("💸 [PaymentEventTranslator] REFUNDED 상태 감지 - 결제 환불 이벤트 발행 시작");
                    publishRefundedEvent(event);
                    break;
                default:
                    log.debug("🔍 [PaymentEventTranslator] 이벤트 변환 대상이 아닌 상태 - status: {}", event.getNewStatus());
            }
        } catch (Exception e) {
            log.error("❌ [PaymentEventTranslator] 이벤트 처리 중 오류 발생 - paymentId: {}, reservationId: {}", 
                event.getPaymentId(), event.getReservationId(), e);
            // 예외를 전파하지 않고 로그만 남김
        }
    }
    
    private void publishCompletedEvent(PaymentStateChangedEvent event) {
        BookingPaymentCompletedEvent completedEvent = BookingPaymentCompletedEvent.builder()
                .paymentId(event.getPaymentId())
                .reservationId(event.getReservationId())
                .completedAt(event.getChangedAt())
                .build();
        
        eventPublisher.publishEvent(completedEvent);
        log.info("🎊 [PaymentEventTranslator] BookingPaymentCompletedEvent 발행 완료 - paymentId: {}, reservationId: {}", 
            event.getPaymentId(), event.getReservationId());
    }
    
    private void publishFailedEvent(PaymentStateChangedEvent event) {
        BookingPaymentFailedEvent failedEvent = BookingPaymentFailedEvent.builder()
                .paymentId(event.getPaymentId())
                .reservationId(event.getReservationId())
                .failedAt(event.getChangedAt())
                .reason(event.getReason())
                .build();
        
        eventPublisher.publishEvent(failedEvent);
        log.info("결제 실패 이벤트 발행 - reservationId: {}, reason: {}", 
                event.getReservationId(), event.getReason());
    }
    
    private void publishCancelledEvent(PaymentStateChangedEvent event) {
        BookingPaymentCancelledEvent cancelledEvent = BookingPaymentCancelledEvent.builder()
                .paymentId(event.getPaymentId())
                .reservationId(event.getReservationId())
                .cancelledAt(event.getChangedAt())
                .reason(event.getReason())
                .build();
        
        eventPublisher.publishEvent(cancelledEvent);
        log.info("결제 취소 이벤트 발행 - reservationId: {}, reason: {}", 
                event.getReservationId(), event.getReason());
    }
    
    private void publishRefundedEvent(PaymentStateChangedEvent event) {
        BookingPaymentRefundedEvent refundedEvent = BookingPaymentRefundedEvent.builder()
                .paymentId(event.getPaymentId())
                .reservationId(event.getReservationId())
                .refundedAt(event.getChangedAt())
                .reason(event.getReason())
                .build();
        
        eventPublisher.publishEvent(refundedEvent);
        log.info("결제 환불 이벤트 발행 - reservationId: {}, reason: {}", 
                event.getReservationId(), event.getReason());
    }
}