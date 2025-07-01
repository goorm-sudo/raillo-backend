package com.sudo.railo.payment.application.event;

import com.sudo.railo.payment.domain.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트
 * 마일리지 적립/사용을 트리거하는 이벤트
 */
@Getter
@AllArgsConstructor
public class PaymentCompletedEvent {
    
    private final String paymentId;
    private final Long memberId;
    private final BigDecimal amountPaid;
    private final BigDecimal mileageToEarn;
    private final LocalDateTime completedAt;
    private final Payment payment;    // 완전한 결제 정보
    
    /**
     * Payment 엔티티로부터 이벤트 생성
     */
    public static PaymentCompletedEvent from(Payment payment) {
        return new PaymentCompletedEvent(
                payment.getPaymentId().toString(),
                payment.getMemberId(),
                payment.getAmountPaid(),
                payment.getMileageToEarn(),
                payment.getPaidAt(),
                payment
        );
    }
} 