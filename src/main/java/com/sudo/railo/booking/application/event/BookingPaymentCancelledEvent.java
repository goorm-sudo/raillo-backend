package com.sudo.railo.booking.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Booking 도메인 전용 결제 취소 이벤트
 * 
 * Payment 도메인의 PaymentStateChangedEvent로부터 변환되어 생성됩니다.
 * 결제 취소 시 예약 및 티켓 취소에 필요한 정보를 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentCancelledEvent {
    
    private String paymentId;
    private Long reservationId;
    private String reason;
    private LocalDateTime cancelledAt;
    
    /**
     * 간편 생성 팩토리 메서드
     */
    public static BookingPaymentCancelledEvent of(String paymentId, Long reservationId, String reason, LocalDateTime cancelledAt) {
        return BookingPaymentCancelledEvent.builder()
                .paymentId(paymentId)
                .reservationId(reservationId)
                .reason(reason)
                .cancelledAt(cancelledAt)
                .build();
    }
}