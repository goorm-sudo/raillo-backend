package com.sudo.railo.booking.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Booking 도메인 전용 환불 이벤트
 * 
 * Payment 도메인의 PaymentStateChangedEvent로부터 변환되어 생성됩니다.
 * 환불 시 예약 및 티켓 상태 업데이트에 필요한 정보를 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentRefundedEvent {
    
    private String paymentId;
    private Long reservationId;
    private String reason;
    private LocalDateTime refundedAt;
    
    /**
     * 간편 생성 팩토리 메서드
     */
    public static BookingPaymentRefundedEvent of(String paymentId, Long reservationId, String reason, LocalDateTime refundedAt) {
        return BookingPaymentRefundedEvent.builder()
                .paymentId(paymentId)
                .reservationId(reservationId)
                .reason(reason)
                .refundedAt(refundedAt)
                .build();
    }
}