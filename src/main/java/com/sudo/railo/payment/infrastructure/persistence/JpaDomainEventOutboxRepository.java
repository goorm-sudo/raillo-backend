package com.sudo.railo.payment.infrastructure.persistence;

import com.sudo.railo.payment.domain.entity.DomainEventOutbox;
import com.sudo.railo.payment.domain.repository.DomainEventOutboxRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DomainEventOutbox JPA Repository 구현체
 * Outbox Pattern을 통한 안정적인 이벤트 처리
 */
@Repository
public interface JpaDomainEventOutboxRepository 
        extends JpaRepository<DomainEventOutbox, String>, DomainEventOutboxRepository {
    
    // DomainEventOutboxRepository의 모든 메서드는 JPA가 자동 구현
    // 추가적인 JPA 특화 메서드가 필요한 경우 여기에 정의
    
    /**
     * 처리 대기 중인 이벤트 조회 (처리 순서 보장)
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.status = 'PENDING' " +
           "ORDER BY e.createdAt ASC")
    List<DomainEventOutbox> findPendingEventsOrderByCreatedAt();
    
    /**
     * 처리 대기 중인 이벤트 조회 (제한된 개수)
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.status = 'PENDING' " +
           "ORDER BY e.createdAt ASC " +
           "LIMIT :limit")
    List<DomainEventOutbox> findPendingEventsWithLimit(@Param("limit") int limit);
    
    /**
     * 재시도 가능한 실패 이벤트 조회
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.status = 'FAILED' " +
           "AND e.retryCount < :maxRetryCount " +
           "ORDER BY e.createdAt ASC")
    List<DomainEventOutbox> findRetryableFailedEvents(@Param("maxRetryCount") int maxRetryCount);
    
    /**
     * 특정 이벤트 타입의 처리 대기 이벤트 조회
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.eventType = :eventType " +
           "AND e.status = 'PENDING' " +
           "ORDER BY e.createdAt ASC")
    List<DomainEventOutbox> findPendingEventsByType(
            @Param("eventType") DomainEventOutbox.EventType eventType);
    
    /**
     * 특정 애그리거트의 이벤트 조회
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.aggregateType = :aggregateType " +
           "AND e.aggregateId = :aggregateId " +
           "ORDER BY e.createdAt DESC")
    List<DomainEventOutbox> findEventsByAggregate(
            @Param("aggregateType") DomainEventOutbox.AggregateType aggregateType,
            @Param("aggregateId") String aggregateId);
    
    /**
     * 특정 애그리거트의 최근 이벤트 조회
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.aggregateType = :aggregateType " +
           "AND e.aggregateId = :aggregateId " +
           "AND e.status = 'COMPLETED' " +
           "ORDER BY e.createdAt DESC " +
           "LIMIT 1")
    Optional<DomainEventOutbox> findLatestCompletedEventByAggregate(
            @Param("aggregateType") DomainEventOutbox.AggregateType aggregateType,
            @Param("aggregateId") String aggregateId);
    
    /**
     * 처리 중인 이벤트 조회 (오래된 순)
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.status = 'PROCESSING' " +
           "ORDER BY e.updatedAt ASC")
    List<DomainEventOutbox> findProcessingEventsOrderByUpdatedAt();
    
    /**
     * 특정 시간 이전의 완료된 이벤트 조회 (정리용)
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.status = 'COMPLETED' " +
           "AND e.processedAt < :beforeTime")
    List<DomainEventOutbox> findCompletedEventsBeforeTime(
            @Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 오래된 완료 이벤트 삭제 (배치 작업용)
     */
    @Override
    @Modifying
    @Query("DELETE FROM DomainEventOutbox e " +
           "WHERE e.status = 'COMPLETED' " +
           "AND e.processedAt < :beforeTime")
    int deleteCompletedEventsBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 이벤트 처리 통계 조회
     */
    @Override
    @Query("SELECT new map(" +
           "COUNT(CASE WHEN e.status = 'PENDING' THEN 1 END) as pendingCount, " +
           "COUNT(CASE WHEN e.status = 'PROCESSING' THEN 1 END) as processingCount, " +
           "COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) as completedCount, " +
           "COUNT(CASE WHEN e.status = 'FAILED' THEN 1 END) as failedCount) " +
           "FROM DomainEventOutbox e " +
           "WHERE e.createdAt >= :fromTime")
    Object getEventStatistics(@Param("fromTime") LocalDateTime fromTime);
    
    /**
     * 이벤트 타입별 통계 조회
     */
    @Override
    @Query("SELECT e.eventType, COUNT(*) " +
           "FROM DomainEventOutbox e " +
           "WHERE e.createdAt >= :fromTime " +
           "GROUP BY e.eventType")
    List<Object[]> getEventTypeStatistics(@Param("fromTime") LocalDateTime fromTime);
    
    /**
     * 특정 시간 범위의 이벤트 조회 (모니터링용)
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY e.createdAt DESC")
    Page<DomainEventOutbox> findEventsByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
    
    /**
     * 실패한 이벤트들 조회 (에러 분석용)
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.status = 'FAILED' " +
           "ORDER BY e.updatedAt DESC")
    Page<DomainEventOutbox> findFailedEvents(Pageable pageable);
    
    /**
     * 특정 이벤트가 이미 처리되었는지 확인
     */
    @Override
    @Query("SELECT COUNT(e) > 0 FROM DomainEventOutbox e " +
           "WHERE e.id = :eventId " +
           "AND e.status = 'COMPLETED'")
    boolean isEventAlreadyProcessed(@Param("eventId") String eventId);
    
    /**
     * 처리 상태별 이벤트 개수 조회
     */
    @Override
    long countByStatus(DomainEventOutbox.EventStatus status);
    
    /**
     * 이벤트 타입별 이벤트 개수 조회
     */
    @Override
    long countByEventType(DomainEventOutbox.EventType eventType);
    
    /**
     * 애그리거트 ID로 이벤트 조회 (테스트 호환용)
     * 애그리거트 타입에 관계없이 ID만으로 조회
     */
    @Override
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.aggregateId = :aggregateId " +
           "ORDER BY e.createdAt DESC")
    List<DomainEventOutbox> findByAggregateId(@Param("aggregateId") String aggregateId);
    
    /**
     * 특정 시간 이전의 처리 중인 이벤트 조회 (데드락 해결용)
     */
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.status = 'PROCESSING' " +
           "AND e.updatedAt < :timeoutTime " +
           "ORDER BY e.updatedAt ASC")
    List<DomainEventOutbox> findTimeoutProcessingEvents(@Param("timeoutTime") LocalDateTime timeoutTime);
    
    /**
     * 이벤트 처리 상태를 PENDING으로 복원 (타임아웃 복구용)
     */
    @Modifying
    @Query("UPDATE DomainEventOutbox e " +
           "SET e.status = 'PENDING', e.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE e.status = 'PROCESSING' " +
           "AND e.updatedAt < :timeoutTime")
    int resetTimeoutProcessingEventsToPending(@Param("timeoutTime") LocalDateTime timeoutTime);
    
    /**
     * 특정 애그리거트의 최근 N개 이벤트 조회
     */
    @Query("SELECT e FROM DomainEventOutbox e " +
           "WHERE e.aggregateType = :aggregateType " +
           "AND e.aggregateId = :aggregateId " +
           "ORDER BY e.createdAt DESC " +
           "LIMIT :limit")
    List<DomainEventOutbox> findRecentEventsByAggregate(
            @Param("aggregateType") DomainEventOutbox.AggregateType aggregateType,
            @Param("aggregateId") String aggregateId,
            @Param("limit") int limit);
    
    // existsById는 JpaRepository가 기본 제공하므로 별도 선언 불필요
    
    /**
     * 처리 완료까지의 평균 시간 조회 (성능 모니터링용)
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, e.createdAt, e.processedAt)) " +
           "FROM DomainEventOutbox e " +
           "WHERE e.status = 'COMPLETED' " +
           "AND e.processedAt IS NOT NULL " +
           "AND e.createdAt >= :fromTime")
    Double getAverageProcessingTimeInSeconds(@Param("fromTime") LocalDateTime fromTime);
    
    /**
     * 재시도 횟수별 이벤트 개수 조회
     */
    @Query("SELECT e.retryCount, COUNT(*) " +
           "FROM DomainEventOutbox e " +
           "WHERE e.status = 'FAILED' " +
           "GROUP BY e.retryCount " +
           "ORDER BY e.retryCount")
    List<Object[]> getFailedEventCountByRetryCount();
    
    /**
     * 특정 기간의 이벤트 처리 성공률 계산
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(*) as successRate " +
           "FROM DomainEventOutbox e " +
           "WHERE e.createdAt >= :fromTime")
    Double calculateSuccessRate(@Param("fromTime") LocalDateTime fromTime);
    
    /**
     * 이벤트 타입별 최근 처리 시간 조회 (모니터링용)
     */
    @Query("SELECT e.eventType, MAX(e.processedAt) " +
           "FROM DomainEventOutbox e " +
           "WHERE e.status = 'COMPLETED' " +
           "AND e.processedAt IS NOT NULL " +
           "GROUP BY e.eventType")
    List<Object[]> getLastProcessedTimeByEventType();
} 