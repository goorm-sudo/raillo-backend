package com.sudo.railo.payment.application.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트
 * 마일리지 적립을 트리거하는 이벤트
 */
@Getter
@AllArgsConstructor
public class PaymentCompletedEvent {
    
    private final String paymentId;
    private final Long memberId;
    private final BigDecimal amountPaid;
    private final BigDecimal mileageToEarn;
    private final LocalDateTime completedAt;
} 