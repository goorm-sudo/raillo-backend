package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 도메인 이벤트 Outbox 엔티티
 * Outbox Pattern을 통한 안정적인 이벤트 발행 및 처리를 위한 엔티티
 */
@Entity
@Table(
    name = "domain_events_outbox",
    indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at"),
        @Index(name = "idx_outbox_event_type", columnList = "event_type"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
    }
)
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DomainEventOutbox extends BaseEntity {
    
    @Id
    @Column(name = "event_id", length = 36)
    private String id;                  // UUID 형태의 이벤트 ID
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;        // 이벤트 타입
    
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private AggregateType aggregateType; // 애그리거트 타입
    
    @Column(name = "aggregate_id", nullable = false, length = 255)
    private String aggregateId;         // 애그리거트 ID
    
    @Column(name = "event_data", columnDefinition = "JSON")
    private String eventData;           // 이벤트 데이터 (JSON 형태)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status;         // 이벤트 처리 상태
    
    @Column(name = "retry_count", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private int retryCount = 0;         // 재시도 횟수
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;  // 처리 완료 시간
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;        // 에러 메시지
    
    /**
     * 이벤트 타입
     */
    public enum EventType {
        TRAIN_ARRIVED("열차 도착"),
        TRAIN_DELAYED("열차 지연"),
        MILEAGE_EARNING_READY("마일리지 적립 준비"),
        MILEAGE_EARNED("마일리지 적립 완료"),
        DELAY_COMPENSATION_EARNED("지연 보상 마일리지 적립"),
        PAYMENT_STATE_CHANGED("결제 상태 변경");
        
        private final String description;
        
        EventType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 애그리거트 타입
     */
    public enum AggregateType {
        TRAIN_SCHEDULE("열차 스케줄"),
        PAYMENT("결제"),
        MILEAGE_TRANSACTION("마일리지 거래");
        
        private final String description;
        
        AggregateType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 이벤트 처리 상태
     */
    public enum EventStatus {
        PENDING("처리 대기"),
        PROCESSING("처리 중"),
        COMPLETED("처리 완료"),
        FAILED("처리 실패");
        
        private final String description;
        
        EventStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 이벤트 처리 시작
     */
    public void startProcessing() {
        this.status = EventStatus.PROCESSING;
    }
    
    /**
     * 이벤트 처리 완료
     */
    public void complete() {
        this.status = EventStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 이벤트 처리 실패
     */
    public void fail(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 재시도 가능 여부 확인
     * @param maxRetryCount 최대 재시도 횟수
     * @return 재시도 가능 여부
     */
    public boolean canRetry(int maxRetryCount) {
        return this.retryCount < maxRetryCount && this.status == EventStatus.FAILED;
    }
    
    /**
     * 열차 도착 이벤트 생성 팩토리 메서드
     */
    public static DomainEventOutbox createTrainArrivedEvent(
            String eventId,
            String trainScheduleId,
            String eventData) {
        
        return DomainEventOutbox.builder()
                .id(eventId)
                .eventType(EventType.TRAIN_ARRIVED)
                .aggregateType(AggregateType.TRAIN_SCHEDULE)
                .aggregateId(trainScheduleId)
                .eventData(eventData)
                .status(EventStatus.PENDING)
                .retryCount(0)
                .build();
    }
    
    /**
     * 마일리지 적립 이벤트 생성 팩토리 메서드
     */
    public static DomainEventOutbox createMileageEarningEvent(
            String eventId,
            String paymentId,
            String eventData,
            EventType eventType) {
        
        return DomainEventOutbox.builder()
                .id(eventId)
                .eventType(eventType)
                .aggregateType(AggregateType.PAYMENT)
                .aggregateId(paymentId)
                .eventData(eventData)
                .status(EventStatus.PENDING)
                .retryCount(0)
                .build();
    }
    
    /**
     * 결제 상태 변경 이벤트 생성 팩토리 메서드
     */
    public static DomainEventOutbox createPaymentStateChangedEvent(
            String eventId,
            String paymentId,
            String eventData) {
        
        return DomainEventOutbox.builder()
                .id(eventId)
                .eventType(EventType.PAYMENT_STATE_CHANGED)
                .aggregateType(AggregateType.PAYMENT)
                .aggregateId(paymentId)
                .eventData(eventData)
                .status(EventStatus.PENDING)
                .retryCount(0)
                .build();
    }
    
    /**
     * 이벤트 ID 조회 (하위 호환성)
     * getId()의 별칭
     */
    public String getEventId() {
        return id;
    }
} 