package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.payment.application.service.DomainEventOutboxService;
import com.sudo.railo.payment.application.port.in.QueryMileageEarningUseCase;
import com.sudo.railo.payment.application.port.in.ProcessMileageEarningUseCase;
import com.sudo.railo.payment.application.port.in.ManageMileageEarningUseCase;
import com.sudo.railo.payment.application.service.TrainArrivalMonitorService;
import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import com.sudo.railo.payment.success.PaymentSuccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 마일리지 적립 시스템 REST API 컨트롤러
 * 사용자용 조회 API와 관리자용 관리 API를 제공
 */
@RestController
@RequestMapping("/api/v1/mileage")
@RequiredArgsConstructor
@Slf4j
public class MileageEarningController {
    
    private final QueryMileageEarningUseCase queryMileageEarningUseCase;
    private final ProcessMileageEarningUseCase processMileageEarningUseCase;
    private final ManageMileageEarningUseCase manageMileageEarningUseCase;
    private final DomainEventOutboxService domainEventOutboxService;
    private final TrainArrivalMonitorService trainArrivalMonitorService;
    
    /**
     * 회원의 적립 예정 마일리지 조회
     */
    @GetMapping("/pending/{memberId}")
    public ResponseEntity<SuccessResponse<BigDecimal>> getPendingMileage(
            @PathVariable Long memberId) {
        log.info("적립 예정 마일리지 조회 - 회원ID: {}", memberId);
        
        BigDecimal pendingMileage = queryMileageEarningUseCase.getPendingMileageByMemberId(memberId);
        
        return ResponseEntity.ok(SuccessResponse.of(
                PaymentSuccess.MILEAGE_INQUIRY_SUCCESS, 
                pendingMileage
        ));
    }
    
    /**
     * 회원의 마일리지 적립 스케줄 조회
     */
    @GetMapping("/schedules/{memberId}")
    public ResponseEntity<SuccessResponse<List<MileageEarningSchedule>>> getEarningSchedules(
            @PathVariable Long memberId,
            @RequestParam(required = false) MileageEarningSchedule.EarningStatus status) {
        log.info("마일리지 적립 스케줄 조회 - 회원ID: {}, 상태: {}", memberId, status);
        
        List<MileageEarningSchedule> schedules = 
                queryMileageEarningUseCase.getEarningSchedulesByMemberId(memberId, status);
        
        return ResponseEntity.ok(SuccessResponse.of(
                PaymentSuccess.MILEAGE_SCHEDULE_INQUIRY_SUCCESS, 
                schedules
        ));
    }
    
    /**
     * 특정 결제의 마일리지 적립 스케줄 조회
     */
    @GetMapping("/schedule/payment/{paymentId}")
    public ResponseEntity<SuccessResponse<MileageEarningSchedule>> getEarningScheduleByPayment(
            @PathVariable String paymentId) {
        log.info("결제별 마일리지 적립 스케줄 조회 - 결제ID: {}", paymentId);
        
        return queryMileageEarningUseCase.getEarningScheduleByPaymentId(paymentId)
                .map(schedule -> ResponseEntity.ok(SuccessResponse.of(
                        PaymentSuccess.MILEAGE_SCHEDULE_INQUIRY_SUCCESS, 
                        schedule
                )))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ========== 관리자 전용 API ==========
    
    /**
     * 열차 정시 도착 처리 - 마일리지 즉시 적립
     * 프론트엔드 결제 내역 페이지에서 호출
     */
    @PostMapping("/admin/train/{trainScheduleId}/arrival")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<String>> recordTrainArrival(
            @PathVariable Long trainScheduleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime actualArrivalTime) {
        log.info("열차 정시 도착 처리 - 스케줄ID: {}, 도착시간: {}", 
                trainScheduleId, actualArrivalTime);
        
        // 정상 도착 (지연 없음)
        trainArrivalMonitorService.simulateTrainArrival(trainScheduleId, actualArrivalTime, 0);
        
        return ResponseEntity.ok(SuccessResponse.of(
                PaymentSuccess.TRAIN_ARRIVAL_RECORDED_SUCCESS, 
                String.format("열차 정시 도착 처리 완료 - 해당 예약의 마일리지가 즉시 적립됩니다")
        ));
    }
    
    /**
     * 열차 지연 도착 처리 - 지연 보상 마일리지 자동 추가
     * 지연 20분 이상: 12.5%, 40분 이상: 25%, 60분 이상: 50% 보상
     * 프론트엔드 결제 내역 페이지에서 호출
     */
    @PostMapping("/admin/train/{trainScheduleId}/delay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<String>> recordTrainDelay(
            @PathVariable Long trainScheduleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime actualArrivalTime,
            @RequestParam int delayMinutes) {
        log.info("열차 지연 도착 처리 - 스케줄ID: {}, 도착시간: {}, 지연: {}분", 
                trainScheduleId, actualArrivalTime, delayMinutes);
        
        if (delayMinutes < 0) {
            return ResponseEntity.badRequest().body(SuccessResponse.of(
                    PaymentSuccess.INVALID_REQUEST, 
                    "지연 시간은 0분 이상이어야 합니다"
            ));
        }
        
        trainArrivalMonitorService.simulateTrainArrival(trainScheduleId, actualArrivalTime, delayMinutes);
        
        String compensationMessage = "";
        if (delayMinutes >= 20) {
            compensationMessage = String.format(" + 지연보상 마일리지 자동 추가 (지연 %d분)", delayMinutes);
        }
        
        return ResponseEntity.ok(SuccessResponse.of(
                PaymentSuccess.TRAIN_DELAY_RECORDED_SUCCESS, 
                String.format("열차 지연 도착 처리 완료 - 기본 마일리지 적립%s", compensationMessage)
        ));
    }
    
    /**
     * 마일리지 적립 현황 통계 - 일별/월별 적립 현황 조회
     */
    @GetMapping("/admin/analytics/earning-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getEarningAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        log.info("마일리지 적립 현황 분석 - 분석기간: {} ~ {}", startTime, endTime);
        
        Map<String, Object> analytics = queryMileageEarningUseCase.getEarningStatistics(startTime, endTime);
        
        return ResponseEntity.ok(SuccessResponse.of(
                PaymentSuccess.STATISTICS_INQUIRY_SUCCESS, 
                analytics
        ));
    }
    
    /**
     * 지연 보상 현황 통계 - 지연별 보상 지급 현황 분석
     */
    @GetMapping("/admin/analytics/delay-compensation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getDelayCompensationAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        log.info("지연 보상 현황 분석 - 분석기간: {} ~ {}", startTime, endTime);
        
        Map<String, Object> analytics = queryMileageEarningUseCase.getDelayCompensationStatistics(startTime, endTime);
        
        return ResponseEntity.ok(SuccessResponse.of(
                PaymentSuccess.STATISTICS_INQUIRY_SUCCESS, 
                analytics
        ));
    }
    
    /**
     * 시스템 이벤트 처리 현황 - Outbox 패턴 이벤트 처리 상태 모니터링
     */
    @GetMapping("/admin/system/event-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getSystemEventStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime) {
        log.info("시스템 이벤트 처리 현황 조회 - 기준시간: {}", fromTime);
        
        Map<String, Object> eventStatus = domainEventOutboxService.getEventStatistics(fromTime);
        
        return ResponseEntity.ok(SuccessResponse.of(
                PaymentSuccess.STATISTICS_INQUIRY_SUCCESS, 
                eventStatus
        ));
    }
    
    /**
     * 마일리지 적립 대기 건 수동 처리
     */
    @PostMapping("/admin/batch/process-schedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<String>> processSchedules(
            @RequestParam(defaultValue = "50") int batchSize) {
        
        log.info("마일리지 적립 대기 건 수동 처리 - 배치크기: {}", batchSize);
        
        ProcessMileageEarningUseCase.ProcessBatchCommand command = 
            new ProcessMileageEarningUseCase.ProcessBatchCommand(batchSize);
        ProcessMileageEarningUseCase.BatchProcessedResult result = 
            processMileageEarningUseCase.processReadySchedules(command);
        int processedCount = result.successCount();
        
        return ResponseEntity.ok(SuccessResponse.of(
                PaymentSuccess.BATCH_PROCESS_SUCCESS, 
                String.format("마일리지 적립 처리 완료 - %d건 처리됨", processedCount)
        ));
    }
    
    /**
     * 완료된 이력 데이터 정리
     */
    @PostMapping("/admin/cleanup/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<String>> cleanupHistory(
            @RequestParam(defaultValue = "30") int eventRetentionDays,
            @RequestParam(defaultValue = "90") int scheduleRetentionDays) {
        
        log.info("완료된 이력 데이터 정리 - 이벤트보관: {}일, 스케줄보관: {}일", 
                eventRetentionDays, scheduleRetentionDays);
        
        int deletedEvents = domainEventOutboxService.cleanupOldCompletedEvents(eventRetentionDays);
        
        ManageMileageEarningUseCase.CleanupOldSchedulesCommand cleanupCommand = 
            new ManageMileageEarningUseCase.CleanupOldSchedulesCommand(scheduleRetentionDays);
        ManageMileageEarningUseCase.CleanupResult cleanupResult = 
            manageMileageEarningUseCase.cleanupOldCompletedSchedules(cleanupCommand);
        int deletedSchedules = cleanupResult.deletedCount();
        
        return ResponseEntity.ok(SuccessResponse.of(
                PaymentSuccess.DATA_CLEANUP_SUCCESS, 
                String.format("데이터 정리 완료 - 이벤트 %d건, 스케줄 %d건 삭제", 
                        deletedEvents, deletedSchedules)
        ));
    }
    
    /**
     * 타임아웃된 이벤트 재처리
     */
    @PostMapping("/admin/recovery/timeout-events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<String>> recoverTimeoutEvents() {
        
        log.info("타임아웃된 이벤트 재처리 실행");
        
        int recoveredCount = domainEventOutboxService.recoverTimeoutProcessingEvents();
        
        return ResponseEntity.ok(SuccessResponse.of(
                PaymentSuccess.EVENT_RECOVERY_SUCCESS, 
                String.format("타임아웃 이벤트 복구 완료 - %d건 복구됨", recoveredCount)
        ));
    }
} 