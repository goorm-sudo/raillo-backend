package com.sudo.railo.payment.application.event;

import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 결제 상태 변경 이벤트
 * 
 * Event Sourcing을 위해 결제의 모든 상태 변경을 기록합니다.
 * 이를 통해 결제의 전체 라이프사이클을 추적하고 감사(audit) 추적이 가능합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PaymentStateChangedEvent {
    
    private String eventId;
    private String paymentId;
    private Long reservationId;
    private PaymentExecutionStatus previousStatus;
    private PaymentExecutionStatus newStatus;
    private LocalDateTime changedAt;
    private String reason;
    private String triggeredBy;  // 시스템/사용자/스케줄러 등
    private Map<String, Object> metadata;  // 추가 정보 (PG 응답, 에러 메시지 등)
    
    /**
     * 상태 변경 이벤트 생성 팩토리 메서드
     */
    public static PaymentStateChangedEvent create(
            String paymentId,
            Long reservationId,
            PaymentExecutionStatus previousStatus,
            PaymentExecutionStatus newStatus,
            String reason,
            String triggeredBy) {
        
        return PaymentStateChangedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .paymentId(paymentId)
                .reservationId(reservationId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .reason(reason)
                .triggeredBy(triggeredBy)
                .build();
    }
    
    /**
     * 메타데이터 추가
     */
    public PaymentStateChangedEvent withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new java.util.HashMap<>(metadata) : null;
        return this;
    }
    
    /**
     * 이벤트가 실패 상태로의 변경인지 확인
     */
    public boolean isFailureTransition() {
        return newStatus == PaymentExecutionStatus.FAILED || 
               newStatus == PaymentExecutionStatus.CANCELLED;
    }
    
    /**
     * 이벤트가 성공 상태로의 변경인지 확인
     */
    public boolean isSuccessTransition() {
        return newStatus == PaymentExecutionStatus.SUCCESS;
    }
    
    /**
     * 환불 관련 상태 변경인지 확인
     */
    public boolean isRefundTransition() {
        return newStatus == PaymentExecutionStatus.REFUNDED;
    }
}