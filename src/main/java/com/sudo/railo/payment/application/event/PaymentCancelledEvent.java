package com.sudo.railo.payment.application.event;

import com.sudo.railo.payment.domain.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 취소 이벤트
 * 마일리지 복구/회수를 트리거하는 이벤트
 */
@Getter
@AllArgsConstructor
public class PaymentCancelledEvent {
    
    private final String paymentId;
    private final Long memberId;
    private final BigDecimal amountCancelled;
    private final BigDecimal mileagePointsUsed;
    private final BigDecimal mileageToEarn;
    private final LocalDateTime cancelledAt;
    private final Payment payment;    // 완전한 결제 정보
    private final String cancelReason;
    
    /**
     * Payment 엔티티로부터 이벤트 생성
     */
    public static PaymentCancelledEvent from(Payment payment, String cancelReason) {
        return new PaymentCancelledEvent(
                payment.getPaymentId().toString(),
                payment.getMemberId(),
                payment.getAmountPaid(),
                payment.getMileagePointsUsed(),
                payment.getMileageToEarn(),
                LocalDateTime.now(),
                payment,
                cancelReason
        );
    }
} 