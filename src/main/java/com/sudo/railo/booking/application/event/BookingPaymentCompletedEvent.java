package com.sudo.railo.booking.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Booking 도메인 전용 결제 완료 이벤트
 * 
 * Payment 도메인의 PaymentStateChangedEvent로부터 변환되어 생성됩니다.
 * Booking 도메인에서 필요한 최소한의 정보만 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentCompletedEvent {
    
    private String paymentId;
    private Long reservationId;
    private LocalDateTime completedAt;
    
    /**
     * 간편 생성 팩토리 메서드
     */
    public static BookingPaymentCompletedEvent of(String paymentId, Long reservationId, LocalDateTime completedAt) {
        return BookingPaymentCompletedEvent.builder()
                .paymentId(paymentId)
                .reservationId(reservationId)
                .completedAt(completedAt)
                .build();
    }
}