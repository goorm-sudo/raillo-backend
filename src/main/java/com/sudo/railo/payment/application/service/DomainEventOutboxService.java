package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.domain.entity.DomainEventOutbox;
import com.sudo.railo.payment.domain.repository.DomainEventOutboxRepository;
import com.sudo.railo.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 도메인 이벤트 Outbox 서비스
 * Outbox Pattern을 통한 안정적인 이벤트 발행 및 처리를 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DomainEventOutboxService {
    
    private final DomainEventOutboxRepository domainEventOutboxRepository;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int PROCESSING_TIMEOUT_MINUTES = 10;
    
    /**
     * 열차 도착 이벤트 발행
     */
    @Transactional
    public void publishTrainArrivedEvent(Long trainScheduleId, LocalDateTime actualArrivalTime) {
        log.info("열차 도착 이벤트 발행 - 열차스케줄ID: {}, 도착시간: {}", trainScheduleId, actualArrivalTime);
        
        DomainEventOutbox event = DomainEventOutbox.builder()
                .id(generateEventId())
                .eventType(DomainEventOutbox.EventType.TRAIN_ARRIVED)
                .aggregateType(DomainEventOutbox.AggregateType.TRAIN_SCHEDULE)
                .aggregateId(trainScheduleId.toString())
                .eventData(String.format("{\"trainScheduleId\":%d,\"actualArrivalTime\":\"%s\"}", 
                          trainScheduleId, actualArrivalTime))
                .status(DomainEventOutbox.EventStatus.PENDING)
                .retryCount(0)
                .build();
        
        domainEventOutboxRepository.save(event);
        
        // 트랜잭션 커밋 후 비동기 처리 등록
        registerPostCommitCallback(() -> log.debug("열차 도착 이벤트 발행 완료 - EventID: {}", event.getId()));
        
        log.debug("열차 도착 이벤트 저장 완료 - EventID: {}", event.getId());
    }
    
    /**
     * 열차 지연 이벤트 발행
     */
    @Transactional
    public void publishTrainDelayedEvent(Long trainScheduleId, int delayMinutes, LocalDateTime actualArrivalTime) {
        log.info("열차 지연 이벤트 발행 - 열차스케줄ID: {}, 지연시간: {}분, 도착시간: {}", 
                trainScheduleId, delayMinutes, actualArrivalTime);
        
        DomainEventOutbox event = DomainEventOutbox.builder()
                .id(generateEventId())
                .eventType(DomainEventOutbox.EventType.TRAIN_DELAYED)
                .aggregateType(DomainEventOutbox.AggregateType.TRAIN_SCHEDULE)
                .aggregateId(trainScheduleId.toString())
                .eventData(String.format("{\"trainScheduleId\":%d,\"delayMinutes\":%d,\"actualArrivalTime\":\"%s\"}", 
                          trainScheduleId, delayMinutes, actualArrivalTime))
                .status(DomainEventOutbox.EventStatus.PENDING)
                .retryCount(0)
                .build();
        
        domainEventOutboxRepository.save(event);
        log.debug("열차 지연 이벤트 저장 완료 - EventID: {}", event.getId());
    }
    
    /**
     * 마일리지 적립 준비 이벤트 발행
     */
    @Transactional
    public void publishMileageEarningReadyEvent(Long earningScheduleId, Long memberId, String paymentId) {
        log.info("마일리지 적립 준비 이벤트 발행 - 적립스케줄ID: {}, 회원ID: {}, 결제ID: {}", 
                earningScheduleId, memberId, paymentId);
        
        DomainEventOutbox event = DomainEventOutbox.builder()
                .id(generateEventId())
                .eventType(DomainEventOutbox.EventType.MILEAGE_EARNING_READY)
                .aggregateType(DomainEventOutbox.AggregateType.PAYMENT)
                .aggregateId(paymentId)
                .eventData(String.format("{\"earningScheduleId\":%d,\"memberId\":%d,\"paymentId\":\"%s\"}", 
                          earningScheduleId, memberId, paymentId))
                .status(DomainEventOutbox.EventStatus.PENDING)
                .retryCount(0)
                .build();
        
        domainEventOutboxRepository.save(event);
        log.debug("마일리지 적립 준비 이벤트 저장 완료 - EventID: {}", event.getId());
    }
    
    /**
     * 마일리지 적립 완료 이벤트 발행
     */
    @Transactional
    public void publishMileageEarnedEvent(Long transactionId, Long memberId, String amount, String earningType) {
        log.info("마일리지 적립 완료 이벤트 발행 - 거래ID: {}, 회원ID: {}, 금액: {}, 타입: {}", 
                transactionId, memberId, amount, earningType);
        
        DomainEventOutbox event = DomainEventOutbox.builder()
                .id(generateEventId())
                .eventType(DomainEventOutbox.EventType.MILEAGE_EARNED)
                .aggregateType(DomainEventOutbox.AggregateType.MILEAGE_TRANSACTION)
                .aggregateId(transactionId.toString())
                .eventData(String.format("{\"transactionId\":%d,\"memberId\":%d,\"amount\":\"%s\",\"earningType\":\"%s\"}", 
                          transactionId, memberId, amount, earningType))
                .status(DomainEventOutbox.EventStatus.PENDING)
                .retryCount(0)
                .build();
        
        domainEventOutboxRepository.save(event);
        log.debug("마일리지 적립 완료 이벤트 저장 완료 - EventID: {}", event.getId());
    }
    
    /**
     * 지연 보상 지급 완료 이벤트 발행
     */
    @Transactional
    public void publishDelayCompensationEarnedEvent(Long transactionId, Long memberId, String compensationAmount, int delayMinutes) {
        log.info("지연 보상 지급 완료 이벤트 발행 - 거래ID: {}, 회원ID: {}, 보상금액: {}, 지연시간: {}분", 
                transactionId, memberId, compensationAmount, delayMinutes);
        
        DomainEventOutbox event = DomainEventOutbox.builder()
                .id(generateEventId())
                .eventType(DomainEventOutbox.EventType.DELAY_COMPENSATION_EARNED)
                .aggregateType(DomainEventOutbox.AggregateType.MILEAGE_TRANSACTION)
                .aggregateId(transactionId.toString())
                .eventData(String.format("{\"transactionId\":%d,\"memberId\":%d,\"compensationAmount\":\"%s\",\"delayMinutes\":%d}", 
                          transactionId, memberId, compensationAmount, delayMinutes))
                .status(DomainEventOutbox.EventStatus.PENDING)
                .retryCount(0)
                .build();
        
        domainEventOutboxRepository.save(event);
        log.debug("지연 보상 지급 완료 이벤트 저장 완료 - EventID: {}", event.getId());
    }
    
    /**
     * 처리 대기 중인 이벤트 조회 (배치 처리용)
     */
    @Transactional(readOnly = true)
    public List<DomainEventOutbox> getPendingEvents(int limit) {
        log.debug("처리 대기 이벤트 조회 - 제한: {}개", limit);
        
        return domainEventOutboxRepository.findPendingEventsWithLimit(limit);
    }
    
    /**
     * 이벤트 처리 시작 (상태를 PROCESSING으로 변경)
     */
    @Transactional
    public boolean startProcessingEvent(String eventId) {
        log.debug("이벤트 처리 시작 - EventID: {}", eventId);
        
        return domainEventOutboxRepository.findById(eventId)
                .map(event -> {
                    if (event.getStatus() == DomainEventOutbox.EventStatus.PENDING) {
                        event.startProcessing();
                        domainEventOutboxRepository.save(event);
                        log.debug("이벤트 처리 상태 변경 완료 - EventID: {}", eventId);
                        return true;
                    } else {
                        log.warn("이미 처리 중이거나 완료된 이벤트 - EventID: {}, 현재상태: {}", eventId, event.getStatus());
                        return false;
                    }
                })
                .orElseThrow(() -> new PaymentException("이벤트를 찾을 수 없습니다 - EventID: " + eventId));
    }
    
    /**
     * 이벤트 처리 완료
     */
    @Transactional
    public void markEventCompleted(String eventId) {
        log.debug("이벤트 처리 완료 - EventID: {}", eventId);
        
        domainEventOutboxRepository.findById(eventId)
                .ifPresent(event -> {
                    event.complete();
                    domainEventOutboxRepository.save(event);
                    log.info("이벤트 처리 완료 - EventID: {}, 타입: {}", eventId, event.getEventType());
                });
    }
    
    /**
     * 이벤트 처리 실패
     */
    @Transactional
    public void markEventFailed(String eventId, String errorMessage) {
        log.error("이벤트 처리 실패 - EventID: {}, 오류: {}", eventId, errorMessage);
        
        domainEventOutboxRepository.findById(eventId)
                .ifPresent(event -> {
                    event.fail(errorMessage);
                    domainEventOutboxRepository.save(event);
                    
                    // 최대 재시도 횟수 초과 시 알림
                    if (event.getRetryCount() >= MAX_RETRY_COUNT) {
                        log.error("최대 재시도 횟수 초과 - EventID: {}, 재시도횟수: {}", eventId, event.getRetryCount());
                        // TODO: 알림 시스템 연동 (Slack, Email 등)
                    }
                });
    }
    
    /**
     * 재시도 가능한 실패 이벤트 조회
     */
    @Transactional(readOnly = true)
    public List<DomainEventOutbox> getRetryableFailedEvents() {
        log.debug("재시도 가능한 실패 이벤트 조회 - 최대재시도: {}", MAX_RETRY_COUNT);
        
        return domainEventOutboxRepository.findRetryableFailedEvents(MAX_RETRY_COUNT);
    }
    
    /**
     * 타임아웃된 처리 중 이벤트 복구
     */
    @Transactional
    public int recoverTimeoutProcessingEvents() {
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(PROCESSING_TIMEOUT_MINUTES);
        
        log.info("타임아웃된 처리 중 이벤트 복구 시작 - 타임아웃 기준: {} 이전", timeoutTime);
        
        int recoveredCount = domainEventOutboxRepository.resetTimeoutProcessingEventsToPending(timeoutTime);
        
        if (recoveredCount > 0) {
            log.warn("타임아웃된 이벤트 복구 완료 - 복구된 이벤트 수: {}", recoveredCount);
        } else {
            log.debug("타임아웃된 이벤트 없음");
        }
        
        return recoveredCount;
    }
    
    /**
     * 이벤트 처리 통계 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEventStatistics(LocalDateTime fromTime) {
        log.debug("이벤트 처리 통계 조회 - 기준시간: {}", fromTime);
        
        Object statisticsObj = domainEventOutboxRepository.getEventStatistics(fromTime);
        
        if (statisticsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> statistics = (Map<String, Object>) statisticsObj;
            log.debug("이벤트 통계 조회 완료 - 대기: {}, 처리중: {}, 완료: {}, 실패: {}", 
                    statistics.get("pendingCount"), statistics.get("processingCount"), 
                    statistics.get("completedCount"), statistics.get("failedCount"));
            return statistics;
        }
        
        return Map.of(
                "pendingCount", 0L,
                "processingCount", 0L,
                "completedCount", 0L,
                "failedCount", 0L
        );
    }
    
    /**
     * 오래된 완료 이벤트 정리 (배치 작업용)
     */
    @Transactional
    public int cleanupOldCompletedEvents(int retentionDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        
        log.info("완료된 이벤트 정리 시작 - 보관기간: {}일, 기준시간: {}", retentionDays, cutoffTime);
        
        int deletedCount = domainEventOutboxRepository.deleteCompletedEventsBeforeTime(cutoffTime);
        
        log.info("완료된 이벤트 정리 완료 - 삭제된 이벤트 수: {}", deletedCount);
        
        return deletedCount;
    }
    
    /**
     * 이벤트 ID 생성
     */
    private String generateEventId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 트랜잭션 커밋 후 콜백 등록
     */
    private void registerPostCommitCallback(Runnable callback) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    callback.run();
                }
            });
        }
    }
} 