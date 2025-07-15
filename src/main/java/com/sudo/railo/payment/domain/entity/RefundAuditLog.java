package com.sudo.railo.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 환불 감사 로그
 * 
 * 토스/당근 스타일로 중요한 환불 이벤트만 선택적으로 저장합니다.
 * 트랜잭션과 독립적으로 저장되어 실패해도 로그는 남습니다.
 */
@Entity
@Table(name = "refund_audit_logs", indexes = {
    @Index(name = "idx_payment_id", columnList = "payment_id"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RefundAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    
    @Column(name = "reservation_id")
    private Long reservationId;
    
    @Column(name = "member_id")
    private Long memberId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;
    
    @Column(name = "event_reason", length = 500)
    private String eventReason;
    
    @Column(name = "event_detail", columnDefinition = "TEXT")
    private String eventDetail;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 감사 이벤트 유형
     * 중요한 이벤트만 선택적으로 기록
     */
    public enum AuditEventType {
        REFUND_DENIED_AFTER_ARRIVAL("도착 후 환불 거부"),
        REFUND_DENIED_DUPLICATE("중복 환불 거부"),
        REFUND_UNKNOWN_STATE("Unknown 상태 발생"),
        REFUND_FAILED_PG_ERROR("PG사 오류로 환불 실패"),
        REFUND_RETRY_SUCCESS("재시도 성공"),
        REFUND_MANUAL_INTERVENTION("수동 개입 필요");
        
        private final String description;
        
        AuditEventType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 빠른 생성을 위한 팩토리 메서드
     */
    public static RefundAuditLog createDeniedLog(
            Long paymentId, 
            Long reservationId,
            Long memberId,
            AuditEventType eventType,
            String reason,
            String detail) {
        
        return RefundAuditLog.builder()
                .paymentId(paymentId)
                .reservationId(reservationId)
                .memberId(memberId)
                .eventType(eventType)
                .eventReason(reason)
                .eventDetail(detail)
                .build();
    }
}