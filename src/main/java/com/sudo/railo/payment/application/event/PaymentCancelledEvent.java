package com.sudo.railo.payment.application.event;

import com.sudo.railo.payment.domain.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 취소 완료 이벤트
 * Booking 도메인에서 예약 상태를 업데이트하기 위해 발행
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelledEvent {
    
    private Long paymentId;
    private Long reservationId;
    private String externalOrderId;
    private Long memberId;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    
    // 마일리지 복구 정보
    private BigDecimal mileageToRestore;      // 복구할 마일리지 (사용한 것)
    private BigDecimal mileageEarnedToCancel; // 취소할 적립 예정 마일리지
    
    // Payment 엔티티 참조 (이벤트 처리를 위해)
    private Payment payment;
    
    /**
     * Payment 엔티티로부터 취소 이벤트 생성
     */
    public static PaymentCancelledEvent from(Payment payment, String cancelReason) {
        return PaymentCancelledEvent.builder()
                .paymentId(payment.getId())
                .reservationId(payment.getReservationId())
                .externalOrderId(payment.getExternalOrderId())
                .memberId(payment.getMemberId())
                .cancelReason(cancelReason)
                .cancelledAt(payment.getCancelledAt())
                .mileageToRestore(payment.getMileagePointsUsed())
                .mileageEarnedToCancel(payment.getMileageToEarn())
                .payment(payment)
                .build();
    }
}