package com.sudo.railo.payment.domain.repository;

import com.sudo.railo.payment.domain.entity.DomainEventOutbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 도메인 이벤트 Outbox Repository
 * Outbox Pattern을 통한 안정적인 이벤트 처리를 위한 Repository
 */
public interface DomainEventOutboxRepository {
    
    /**
     * 처리 대기 중인 이벤트 조회 (처리 순서 보장)
     */
    List<DomainEventOutbox> findPendingEventsOrderByCreatedAt();
    
    /**
     * 처리 대기 중인 이벤트 조회 (제한된 개수)
     */
    List<DomainEventOutbox> findPendingEventsWithLimit(int limit);
    
    /**
     * 재시도 가능한 실패 이벤트 조회
     */
    List<DomainEventOutbox> findRetryableFailedEvents(int maxRetryCount);
    
    /**
     * 특정 이벤트 타입의 처리 대기 이벤트 조회
     */
    List<DomainEventOutbox> findPendingEventsByType(
            DomainEventOutbox.EventType eventType);
    
    /**
     * 특정 애그리거트의 이벤트 조회
     */
    List<DomainEventOutbox> findEventsByAggregate(
            DomainEventOutbox.AggregateType aggregateType,
            String aggregateId);
    
    /**
     * 특정 애그리거트의 최근 이벤트 조회
     */
    Optional<DomainEventOutbox> findLatestCompletedEventByAggregate(
            DomainEventOutbox.AggregateType aggregateType,
            String aggregateId);
    
    /**
     * 처리 중인 이벤트 조회 (오래된 순)
     */
    List<DomainEventOutbox> findProcessingEventsOrderByUpdatedAt();
    
    /**
     * 특정 시간 이전의 완료된 이벤트 조회 (정리용)
     */
    List<DomainEventOutbox> findCompletedEventsBeforeTime(
            LocalDateTime beforeTime);
    
    /**
     * 오래된 완료 이벤트 삭제 (배치 작업용)
     */
    int deleteCompletedEventsBeforeTime(LocalDateTime beforeTime);
    
    /**
     * 타임아웃된 처리 중 이벤트를 PENDING으로 복원 (복구용)
     */
    int resetTimeoutProcessingEventsToPending(LocalDateTime timeoutTime);
    
    /**
     * 이벤트 처리 통계 조회
     */
    Object getEventStatistics(LocalDateTime fromTime);
    
    /**
     * 이벤트 타입별 통계 조회
     */
    List<Object[]> getEventTypeStatistics(LocalDateTime fromTime);
    
    /**
     * 특정 시간 범위의 이벤트 조회 (모니터링용)
     */
    Page<DomainEventOutbox> findEventsByTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable);
    
    /**
     * 실패한 이벤트들 조회 (에러 분석용)
     */
    Page<DomainEventOutbox> findFailedEvents(Pageable pageable);
    
    /**
     * 특정 이벤트가 이미 처리되었는지 확인
     */
    boolean isEventAlreadyProcessed(String eventId);
    
    /**
     * 처리 상태별 이벤트 개수 조회
     */
    long countByStatus(DomainEventOutbox.EventStatus status);
    
    /**
     * 이벤트 타입별 이벤트 개수 조회
     */
    long countByEventType(DomainEventOutbox.EventType eventType);
    
    /**
     * 애그리거트 ID로 이벤트 조회 (테스트 호환용)
     * 애그리거트 타입에 관계없이 ID만으로 조회
     */
    List<DomainEventOutbox> findByAggregateId(String aggregateId);
    
    /**
     * ID로 이벤트 조회
     */
    Optional<DomainEventOutbox> findById(String id);
    
    /**
     * 이벤트 저장
     */
    DomainEventOutbox save(DomainEventOutbox event);
    
    /**
     * 이벤트 삭제
     */
    void delete(DomainEventOutbox event);
    
    /**
     * ID로 이벤트 삭제
     */
    void deleteById(String id);
    
    /**
     * ID 존재 여부 확인
     */
    boolean existsById(String id);
} 