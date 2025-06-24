package com.sudo.railo.payment.application.event;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentEvent {
    
    private String eventId;
    private String eventType;
    private String paymentId;
    private String externalOrderId;
    private String userId;
    private LocalDateTime timestamp;
    private Map<String, Object> eventData;
    
    public static PaymentEvent createCalculationEvent(String calculationId, String orderId, String userId) {
        return PaymentEvent.builder()
            .eventId(java.util.UUID.randomUUID().toString())
            .eventType("PAYMENT_CALCULATION_CREATED")
            .paymentId(calculationId)
            .externalOrderId(orderId)
            .userId(userId)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static PaymentEvent createExecutionEvent(String paymentId, String orderId, String userId) {
        return PaymentEvent.builder()
            .eventId(java.util.UUID.randomUUID().toString())
            .eventType("PAYMENT_EXECUTION_STARTED")
            .paymentId(paymentId)
            .externalOrderId(orderId)
            .userId(userId)
            .timestamp(LocalDateTime.now())
            .build();
    }
} 