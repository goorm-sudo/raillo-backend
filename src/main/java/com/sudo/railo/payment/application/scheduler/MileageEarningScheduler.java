package com.sudo.railo.payment.application.scheduler;

import com.sudo.railo.payment.application.service.DomainEventOutboxService;
import com.sudo.railo.payment.application.port.in.ProcessMileageEarningUseCase;
import com.sudo.railo.payment.application.port.in.QueryMileageEarningUseCase;
import com.sudo.railo.payment.application.port.in.ManageMileageEarningUseCase;
import com.sudo.railo.payment.application.service.TrainArrivalMonitorService;
import com.sudo.railo.payment.domain.entity.DomainEventOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 마일리지 적립 스케줄러
 * 실시간 마일리지 적립 시스템의 핵심 배치 작업을 담당
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "raillo.mileage.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MileageEarningScheduler {
    
    private final ProcessMileageEarningUseCase processMileageEarningUseCase;
    private final QueryMileageEarningUseCase queryMileageEarningUseCase;
    private final ManageMileageEarningUseCase manageMileageEarningUseCase;
    private final DomainEventOutboxService domainEventOutboxService;
    private final TrainArrivalMonitorService trainArrivalMonitorService;
    
    private static final int BATCH_SIZE = 50;
    private static final int EVENT_BATCH_SIZE = 20;
    
    /**
     * 실시간 열차 도착 모니터링
     * 매 1분마다 실행하여 도착한 열차를 체크하고 마일리지 적립 스케줄을 준비 상태로 변경
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void monitorTrainArrivals() {
        try {
            log.debug("실시간 열차 도착 모니터링 시작");
            
            int processedCount = trainArrivalMonitorService.checkAndProcessArrivedTrains();
            
            if (processedCount > 0) {
                log.info("열차 도착 모니터링 완료 - 처리된 열차 수: {}", processedCount);
            } else {
                log.debug("도착한 열차 없음");
            }
            
        } catch (Exception e) {
            log.error("열차 도착 모니터링 중 오류 발생", e);
        }
    }
    
    /**
     * 마일리지 적립 스케줄 배치 처리
     * 매 1분마다 실행하여 준비된 마일리지 적립 스케줄을 처리
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void processReadyEarningSchedules() {
        try {
            log.debug("마일리지 적립 스케줄 배치 처리 시작");
            
            ProcessMileageEarningUseCase.ProcessBatchCommand command = 
                new ProcessMileageEarningUseCase.ProcessBatchCommand(BATCH_SIZE);
            ProcessMileageEarningUseCase.BatchProcessedResult result = 
                processMileageEarningUseCase.processReadySchedules(command);
            int processedCount = result.successCount();
            
            if (processedCount > 0) {
                log.info("마일리지 적립 스케줄 처리 완료 - 처리된 스케줄 수: {}", processedCount);
            } else {
                log.debug("처리할 마일리지 적립 스케줄 없음");
            }
            
        } catch (Exception e) {
            log.error("마일리지 적립 스케줄 처리 중 오류 발생", e);
        }
    }
    
    /**
     * Outbox 이벤트 처리
     * 매 30초마다 실행하여 대기 중인 도메인 이벤트를 처리
     */
    @Scheduled(fixedRate = 30000) // 30초마다 실행
    public void processOutboxEvents() {
        try {
            log.debug("Outbox 이벤트 처리 시작");
            
            List<DomainEventOutbox> pendingEvents = domainEventOutboxService.getPendingEvents(EVENT_BATCH_SIZE);
            
            if (pendingEvents.isEmpty()) {
                log.debug("처리할 Outbox 이벤트 없음");
                return;
            }
            
            int processedCount = 0;
            int failedCount = 0;
            
            for (DomainEventOutbox event : pendingEvents) {
                try {
                    // 이벤트 처리 시작
                    boolean started = domainEventOutboxService.startProcessingEvent(event.getId());
                    
                    if (started) {
                        // 실제 이벤트 처리 로직 (현재는 단순히 완료 처리)
                        processEvent(event);
                        
                        // 처리 완료 표시
                        domainEventOutboxService.markEventCompleted(event.getId());
                        processedCount++;
                        
                        log.debug("이벤트 처리 완료 - EventID: {}, Type: {}", 
                                event.getId(), event.getEventType());
                    }
                    
                } catch (Exception e) {
                    // 처리 실패 표시
                    domainEventOutboxService.markEventFailed(event.getId(), e.getMessage());
                    failedCount++;
                    
                    log.error("이벤트 처리 실패 - EventID: {}, Type: {}, 오류: {}", 
                            event.getId(), event.getEventType(), e.getMessage(), e);
                }
            }
            
            log.info("Outbox 이벤트 처리 완료 - 성공: {}, 실패: {}", processedCount, failedCount);
            
        } catch (Exception e) {
            log.error("Outbox 이벤트 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 실패한 이벤트 재시도 처리
     * 매 5분마다 실행하여 재시도 가능한 실패 이벤트를 다시 처리
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void retryFailedEvents() {
        try {
            log.debug("실패한 이벤트 재시도 처리 시작");
            
            List<DomainEventOutbox> retryableEvents = domainEventOutboxService.getRetryableFailedEvents();
            
            if (retryableEvents.isEmpty()) {
                log.debug("재시도할 실패 이벤트 없음");
                return;
            }
            
            int retryCount = 0;
            
            for (DomainEventOutbox event : retryableEvents) {
                try {
                    // 이벤트를 PENDING 상태로 되돌려서 다시 처리되도록 함
                    boolean started = domainEventOutboxService.startProcessingEvent(event.getId());
                    
                    if (started) {
                        processEvent(event);
                        domainEventOutboxService.markEventCompleted(event.getId());
                        retryCount++;
                        
                        log.info("실패 이벤트 재시도 성공 - EventID: {}, Type: {}", 
                                event.getId(), event.getEventType());
                    }
                    
                } catch (Exception e) {
                    domainEventOutboxService.markEventFailed(event.getId(), e.getMessage());
                    
                    log.warn("실패 이벤트 재시도 실패 - EventID: {}, Type: {}, 재시도횟수: {}", 
                            event.getId(), event.getEventType(), event.getRetryCount() + 1, e);
                }
            }
            
            if (retryCount > 0) {
                log.info("실패한 이벤트 재시도 완료 - 성공한 재시도: {}/{}", retryCount, retryableEvents.size());
            }
            
        } catch (Exception e) {
            log.error("실패한 이벤트 재시도 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 타임아웃된 처리 중 이벤트 복구
     * 매 10분마다 실행하여 처리 중 상태로 오래 남아있는 이벤트를 복구
     */
    @Scheduled(fixedRate = 600000) // 10분마다 실행
    public void recoverTimeoutEvents() {
        try {
            log.debug("타임아웃된 처리 중 이벤트 복구 시작");
            
            int recoveredCount = domainEventOutboxService.recoverTimeoutProcessingEvents();
            
            if (recoveredCount > 0) {
                log.warn("타임아웃된 이벤트 복구 완료 - 복구된 이벤트 수: {}", recoveredCount);
            } else {
                log.debug("타임아웃된 이벤트 없음");
            }
            
        } catch (Exception e) {
            log.error("타임아웃된 이벤트 복구 중 오류 발생", e);
        }
    }
    
    /**
     * 일일 데이터 정리 작업
     * 매일 새벽 2시에 실행하여 오래된 완료 이벤트와 스케줄을 정리
     */
    @Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
    public void dailyDataCleanup() {
        try {
            log.info("일일 데이터 정리 작업 시작");
            
            // 30일 이상 된 완료 이벤트 삭제
            int deletedEvents = domainEventOutboxService.cleanupOldCompletedEvents(30);
            
            // 90일 이상 된 완료 스케줄 삭제
            ManageMileageEarningUseCase.CleanupOldSchedulesCommand cleanupCommand = 
                new ManageMileageEarningUseCase.CleanupOldSchedulesCommand(90);
            ManageMileageEarningUseCase.CleanupResult cleanupResult = 
                manageMileageEarningUseCase.cleanupOldCompletedSchedules(cleanupCommand);
            int deletedSchedules = cleanupResult.deletedCount();
            
            log.info("일일 데이터 정리 작업 완료 - 삭제된 이벤트: {}, 삭제된 스케줄: {}", 
                    deletedEvents, deletedSchedules);
            
        } catch (Exception e) {
            log.error("일일 데이터 정리 작업 중 오류 발생", e);
        }
    }
    
    /**
     * 시간별 통계 생성
     * 매 시간 정각에 실행하여 마일리지 적립 통계를 생성
     */
    @Scheduled(cron = "0 0 * * * *") // 매 시간 정각
    public void generateHourlyStatistics() {
        try {
            log.debug("시간별 통계 생성 시작");
            
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(1);
            
            // 이벤트 처리 통계
            var eventStats = domainEventOutboxService.getEventStatistics(startTime);
            
            // 마일리지 적립 통계
            var earningStats = queryMileageEarningUseCase.getEarningStatistics(startTime, endTime);
            
            // 지연 보상 통계
            var delayStats = queryMileageEarningUseCase.getDelayCompensationStatistics(startTime, endTime);
            
            log.info("시간별 통계 생성 완료 - 기간: {} ~ {}, 이벤트: {}, 적립: {}, 지연보상: {}", 
                    startTime, endTime, eventStats, earningStats, delayStats);
            
        } catch (Exception e) {
            log.error("시간별 통계 생성 중 오류 발생", e);
        }
    }
    
    /**
     * 개별 이벤트 처리 로직
     */
    private void processEvent(DomainEventOutbox event) {
        log.debug("이벤트 처리 중 - EventID: {}, Type: {}, Data: {}", 
                event.getId(), event.getEventType(), event.getEventData());
        
        // 실제 이벤트 처리 로직은 이벤트 타입에 따라 분기
        // 현재는 단순히 로깅만 수행
        switch (event.getEventType()) {
            case TRAIN_ARRIVED:
                log.debug("열차 도착 이벤트 처리 완료");
                break;
            case TRAIN_DELAYED:
                log.debug("열차 지연 이벤트 처리 완료");
                break;
            case MILEAGE_EARNING_READY:
                log.debug("마일리지 적립 준비 이벤트 처리 완료");
                break;
            case MILEAGE_EARNED:
                log.debug("마일리지 적립 완료 이벤트 처리 완료");
                break;
            case DELAY_COMPENSATION_EARNED:
                log.debug("지연 보상 지급 완료 이벤트 처리 완료");
                break;
            default:
                log.warn("알 수 없는 이벤트 타입 - Type: {}", event.getEventType());
        }
    }
} 