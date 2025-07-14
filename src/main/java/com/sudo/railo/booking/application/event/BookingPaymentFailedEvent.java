package com.sudo.railo.booking.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Booking 도메인 전용 결제 실패 이벤트
 * 
 * Payment 도메인의 PaymentStateChangedEvent로부터 변환되어 생성됩니다.
 * 결제 실패 시 예약 상태 업데이트에 필요한 정보를 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentFailedEvent {
    
    private String paymentId;
    private Long reservationId;
    private String reason;
    private LocalDateTime failedAt;
    
    /**
     * 간편 생성 팩토리 메서드
     */
    public static BookingPaymentFailedEvent of(String paymentId, Long reservationId, String reason, LocalDateTime failedAt) {
        return BookingPaymentFailedEvent.builder()
                .paymentId(paymentId)
                .reservationId(reservationId)
                .reason(reason)
                .failedAt(failedAt)
                .build();
    }
    
    /**
     * 실패 사유 조회 (하위 호환성)
     * getReason()의 별칭
     */
    public String getFailureReason() {
        return reason;
    }
}