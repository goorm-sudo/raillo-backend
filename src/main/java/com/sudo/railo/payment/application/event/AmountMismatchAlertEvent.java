package com.sudo.railo.payment.application.event;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 금액 불일치 알림 이벤트
 * PG사에서 확인한 금액과 계산된 금액이 다를 때 발행
 */
@Getter
@Builder
@ToString
public class AmountMismatchAlertEvent {
    
    private final String calculationId;
    private final BigDecimal expectedAmount;
    private final BigDecimal actualAmount;
    private final String pgOrderId;
    private final String pgAuthNumber;
    private final LocalDateTime occurredAt;
    private final String severity; // HIGH, MEDIUM, LOW
    
    public static AmountMismatchAlertEvent create(String calculationId,
                                                   BigDecimal expectedAmount,
                                                   BigDecimal actualAmount,
                                                   String pgOrderId,
                                                   String pgAuthNumber) {
        // 금액 차이에 따른 심각도 결정
        BigDecimal difference = expectedAmount.subtract(actualAmount).abs();
        BigDecimal percentageDiff = difference.divide(expectedAmount, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
        
        String severity;
        if (percentageDiff.compareTo(new BigDecimal("10")) > 0) {
            severity = "HIGH"; // 10% 이상 차이
        } else if (percentageDiff.compareTo(new BigDecimal("5")) > 0) {
            severity = "MEDIUM"; // 5% 이상 차이
        } else {
            severity = "LOW"; // 5% 미만 차이
        }
        
        return AmountMismatchAlertEvent.builder()
                .calculationId(calculationId)
                .expectedAmount(expectedAmount)
                .actualAmount(actualAmount)
                .pgOrderId(pgOrderId)
                .pgAuthNumber(pgAuthNumber)
                .occurredAt(LocalDateTime.now())
                .severity(severity)
                .build();
    }
    
    public BigDecimal getAmountDifference() {
        return expectedAmount.subtract(actualAmount).abs();
    }
    
    public String getAlertMessage() {
        return String.format(
                "[%s] 결제 금액 불일치 감지 - 계산ID: %s, 예상금액: %s원, 실제금액: %s원, 차이: %s원, PG주문번호: %s",
                severity,
                calculationId,
                expectedAmount,
                actualAmount,
                getAmountDifference(),
                pgOrderId
        );
    }
}