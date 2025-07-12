package com.sudo.railo.payment.application.service;

import com.sudo.railo.global.redis.annotation.DistributedLock;
import com.sudo.railo.payment.application.port.in.*;
import com.sudo.railo.payment.application.port.out.*;
import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.domain.service.MileageEarningDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 마일리지 적립 애플리케이션 서비스
 * 
 * 헥사고날 아키텍처의 애플리케이션 계층으로, 유스케이스를 구현하고
 * 포트들을 조합하여 비즈니스 플로우를 실행합니다.
 * 트랜잭션 경계와 분산 락 관리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MileageEarningApplicationService implements 
        CreateMileageEarningScheduleUseCase,
        ProcessMileageEarningUseCase,
        UpdateMileageEarningScheduleUseCase,
        QueryMileageEarningUseCase,
        ManageMileageEarningUseCase {
    
    // 출력 포트
    private final LoadMileageEarningSchedulePort loadSchedulePort;
    private final SaveMileageEarningSchedulePort saveSchedulePort;
    private final MileageEarningEventPort eventPort;
    private final LoadMemberInfoPort memberInfoPort;
    private final LoadTrainSchedulePort trainSchedulePort;
    private final SaveMileageTransactionPort saveMileageTransactionPort;
    
    // 도메인 서비스
    private final MileageEarningDomainService domainService;
    private final MileageTransactionService mileageTransactionService;
    
    @Override
    @Transactional
    public ScheduleCreatedResult createEarningSchedule(CreateScheduleCommand command) {
        log.info("마일리지 적립 스케줄 생성 - 열차스케줄ID: {}, 결제ID: {}, 회원ID: {}, 결제금액: {}", 
                command.trainScheduleId(), command.paymentId(), 
                command.memberId(), command.paymentAmount());
        
        // 노선 정보 조회
        String routeInfo = trainSchedulePort.getRouteInfo(command.trainScheduleId());
        
        // 도메인 서비스를 통해 스케줄 생성
        MileageEarningSchedule schedule = domainService.createNormalEarningSchedule(
            command.trainScheduleId(),
            command.paymentId(),
            command.memberId(),
            command.paymentAmount(),
            command.expectedArrivalTime(),
            routeInfo
        );
        
        // 스케줄 저장
        schedule = saveSchedulePort.save(schedule);
        
        log.info("마일리지 적립 스케줄 생성 완료 - 스케줄ID: {}, 기본적립: {}P", 
                schedule.getId(), schedule.getBaseMileageAmount());
        
        return new ScheduleCreatedResult(
            schedule.getId(),
            schedule.getBaseMileageAmount(),
            schedule.getScheduledEarningTime(),
            schedule.getStatus().name()
        );
    }
    
    @Override
    @Transactional
    public BatchProcessedResult processReadySchedules(ProcessBatchCommand command) {
        log.debug("마일리지 적립 스케줄 배치 처리 시작 - 배치크기: {}", command.batchSize());
        
        LocalDateTime now = LocalDateTime.now();
        // 비관적 락을 사용하여 안전하게 조회
        List<MileageEarningSchedule> readySchedules = 
                loadSchedulePort.findReadySchedulesWithLock(now, command.batchSize());
        
        if (readySchedules.isEmpty()) {
            log.debug("처리할 마일리지 적립 스케줄이 없습니다");
            return new BatchProcessedResult(0, 0, 0);
        }
        
        int successCount = 0;
        int failureCount = 0;
        
        // 각 스케줄을 개별 트랜잭션으로 처리
        for (MileageEarningSchedule schedule : readySchedules) {
            try {
                processEarningSchedule(new ProcessScheduleCommand(schedule.getId()));
                successCount++;
            } catch (Exception e) {
                log.error("마일리지 적립 스케줄 처리 실패 - 스케줄ID: {}, 오류: {}",
                        schedule.getId(), e.getMessage(), e);
                failureCount++;
            }
        }
        
        log.info("마일리지 적립 스케줄 배치 처리 완료 - 총 처리: {}, 성공: {}, 실패: {}", 
                readySchedules.size(), successCount, failureCount);
        
        return new BatchProcessedResult(readySchedules.size(), successCount, failureCount);
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @DistributedLock(key = "#command.scheduleId()", prefix = "mileage:schedule", waitTime = 3)
    public void processEarningSchedule(ProcessScheduleCommand command) {
        log.debug("마일리지 적립 스케줄 처리 시작 (with lock) - 스케줄ID: {}", command.scheduleId());
        
        // 스케줄 조회
        MileageEarningSchedule schedule = loadSchedulePort.findById(command.scheduleId())
                .orElseThrow(() -> new IllegalArgumentException("스케줄을 찾을 수 없습니다: " + command.scheduleId()));
        
        // 원자적 상태 변경 (READY → BASE_PROCESSING)
        int updated = saveSchedulePort.updateStatusAtomically(
                command.scheduleId(),
                MileageEarningSchedule.EarningStatus.READY,
                MileageEarningSchedule.EarningStatus.BASE_PROCESSING
        );
        
        if (updated == 0) {
            log.debug("스케줄이 이미 처리 중이거나 처리되었습니다 - 스케줄ID: {}", command.scheduleId());
            return;
        }
        
        try {
            processEarningScheduleInternal(schedule);
        } catch (Exception e) {
            handleScheduleFailure(schedule, e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public ScheduleUpdateResult markScheduleReady(MarkScheduleReadyCommand command) {
        log.info("마일리지 적립 스케줄 준비 완료 처리 - 열차스케줄ID: {}, 실제도착시간: {}", 
                command.trainScheduleId(), command.actualArrivalTime());
        
        List<MileageEarningSchedule> schedules = 
                loadSchedulePort.findByTrainScheduleId(command.trainScheduleId());
        
        if (schedules.isEmpty()) {
            log.warn("해당 열차의 마일리지 적립 스케줄이 없습니다 - 열차스케줄ID: {}", command.trainScheduleId());
            return new ScheduleUpdateResult(0, "NO_SCHEDULES");
        }
        
        for (MileageEarningSchedule schedule : schedules) {
            schedule.markReady();
            schedule.updateScheduledEarningTime(command.actualArrivalTime());
            saveSchedulePort.save(schedule);
            
            // 마일리지 적립 준비 이벤트 발행
            eventPort.publishMileageEarningReadyEvent(
                    schedule.getId(),
                    schedule.getMemberId(),
                    String.valueOf(schedule.getId())
            );
            
            log.debug("마일리지 적립 스케줄 READY 상태 변경 완료 - 스케줄ID: {}", schedule.getId());
        }
        
        log.info("열차별 마일리지 적립 스케줄 준비 완료 - 열차스케줄ID: {}, 처리된 스케줄 수: {}", 
                command.trainScheduleId(), schedules.size());
        
        return new ScheduleUpdateResult(schedules.size(), "READY");
    }
    
    @Override
    @Transactional
    public ScheduleUpdateResult updateDelayCompensation(UpdateDelayCompensationCommand command) {
        log.info("지연 보상 마일리지 스케줄 업데이트 - 열차스케줄ID: {}, 지연시간: {}분", 
                command.trainScheduleId(), command.delayMinutes());
        
        List<MileageEarningSchedule> schedules = 
                loadSchedulePort.findByTrainScheduleId(command.trainScheduleId());
        
        if (schedules.isEmpty()) {
            log.warn("해당 열차의 마일리지 적립 스케줄이 없습니다 - 열차스케줄ID: {}", command.trainScheduleId());
            return new ScheduleUpdateResult(0, "NO_SCHEDULES");
        }
        
        // 지연 보상이 필요한지 확인
        if (!domainService.requiresDelayCompensation(command.delayMinutes())) {
            log.info("지연 보상이 필요하지 않습니다 - 지연시간: {}분", command.delayMinutes());
            return new ScheduleUpdateResult(0, "NO_COMPENSATION_REQUIRED");
        }
        
        // 지연 보상 비율 계산
        BigDecimal compensationRate = new BigDecimal(
            String.valueOf(command.delayMinutes() >= 60 ? 0.5 :
                         command.delayMinutes() >= 40 ? 0.25 : 0.125)
        );
        
        for (MileageEarningSchedule schedule : schedules) {
            schedule.updateDelayInfo(command.delayMinutes(), compensationRate);
            schedule.updateScheduledEarningTime(command.actualArrivalTime());
            saveSchedulePort.save(schedule);
            
            log.debug("지연 보상 스케줄 업데이트 완료 - 스케줄ID: {}, 보상금액: {}P", 
                    schedule.getId(), schedule.getDelayCompensationAmount());
        }
        
        log.info("지연 보상 마일리지 스케줄 업데이트 완료 - 열차스케줄ID: {}, 처리된 스케줄 수: {}, 보상비율: {}%", 
                command.trainScheduleId(), schedules.size(), compensationRate.multiply(new BigDecimal("100")));
        
        return new ScheduleUpdateResult(schedules.size(), "DELAY_COMPENSATION_UPDATED");
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPendingMileageByMemberId(Long memberId) {
        log.debug("회원의 적립 예정 마일리지 조회 - 회원ID: {}", memberId);
        return loadSchedulePort.calculatePendingMileageByMemberId(memberId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MileageEarningSchedule> getEarningSchedulesByMemberId(
            Long memberId, MileageEarningSchedule.EarningStatus status) {
        log.debug("회원의 마일리지 적립 스케줄 조회 - 회원ID: {}, 상태: {}", memberId, status);
        
        if (status == null) {
            return loadSchedulePort.findByMemberId(memberId);
        } else {
            return loadSchedulePort.findByMemberIdAndStatus(memberId, status);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<MileageEarningSchedule> getEarningScheduleByPaymentId(String paymentId) {
        log.debug("결제별 마일리지 적립 스케줄 조회 - 결제ID: {}", paymentId);
        return loadSchedulePort.findByPaymentId(paymentId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEarningStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("마일리지 적립 통계 조회 - 기간: {} ~ {}", startTime, endTime);
        
        Object result = loadSchedulePort.getMileageEarningStatistics(startTime, endTime);
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return map;
        }
        return Map.of();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDelayCompensationStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("지연 보상 통계 조회 - 기간: {} ~ {}", startTime, endTime);
        
        Object result = loadSchedulePort.getDelayCompensationStatistics(startTime, endTime);
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return map;
        }
        return Map.of();
    }
    
    /**
     * 마일리지 적립 스케줄 내부 처리 로직
     */
    private void processEarningScheduleInternal(MileageEarningSchedule schedule) {
        log.debug("마일리지 적립 내부 처리 시작 - 스케줄ID: {}", schedule.getId());
        
        // 회원의 현재 마일리지 잔액 조회
        BigDecimal balanceBefore = memberInfoPort.getMileageBalance(schedule.getMemberId());
        log.debug("회원의 현재 마일리지 잔액 - 회원ID: {}, 잔액: {}P", schedule.getMemberId(), balanceBefore);
        
        // 1단계: 기본 마일리지 적립 처리
        MileageTransaction baseTransaction = mileageTransactionService.createBaseEarningTransaction(
                schedule.getMemberId(),
                String.valueOf(schedule.getId()),
                schedule.getBaseMileageAmount(),
                schedule.getTrainScheduleId(),
                schedule.getId(),
                balanceBefore
        );
        
        // 원자적으로 상태와 트랜잭션 ID 업데이트
        MileageEarningSchedule.EarningStatus nextStatus = schedule.hasDelayCompensation() 
                ? MileageEarningSchedule.EarningStatus.BASE_COMPLETED 
                : MileageEarningSchedule.EarningStatus.FULLY_COMPLETED;
        
        int updated = saveSchedulePort.updateWithTransactionAtomically(
                schedule.getId(),
                MileageEarningSchedule.EarningStatus.BASE_PROCESSING,
                nextStatus,
                baseTransaction.getId(),
                !schedule.hasDelayCompensation()
        );
        
        if (updated == 0) {
            throw new IllegalStateException("스케줄 상태 업데이트 실패 - 동시성 문제 발생");
        }
        
        // 기본 적립 완료 이벤트 발행
        eventPort.publishMileageEarnedEvent(
                baseTransaction.getId(),
                baseTransaction.getMemberId(),
                baseTransaction.getPointsAmount().toString(),
                baseTransaction.getEarningType().name()
        );
        
        // 2단계: 지연 보상이 있는 경우 처리
        if (schedule.hasDelayCompensation()) {
            processDelayCompensation(schedule, balanceBefore.add(baseTransaction.getPointsAmount()));
        }
        
        log.info("마일리지 적립 스케줄 처리 완료 - 스케줄ID: {}, 기본적립: {}P, 지연보상: {}P",
                schedule.getId(), schedule.getBaseMileageAmount(), 
                schedule.getDelayCompensationAmount() != null ? schedule.getDelayCompensationAmount() : BigDecimal.ZERO);
    }
    
    /**
     * 지연 보상 마일리지 처리
     */
    private void processDelayCompensation(MileageEarningSchedule schedule, BigDecimal balanceBeforeCompensation) {
        // 상태를 COMPENSATION_PROCESSING으로 변경
        int updated = saveSchedulePort.updateStatusAtomically(
                schedule.getId(),
                MileageEarningSchedule.EarningStatus.BASE_COMPLETED,
                MileageEarningSchedule.EarningStatus.COMPENSATION_PROCESSING
        );
        
        if (updated == 0) {
            log.warn("지연 보상 처리를 시작할 수 없습니다 - 스케줄ID: {}", schedule.getId());
            return;
        }
        
        MileageTransaction compensationTransaction = mileageTransactionService.createDelayCompensationTransaction(
                schedule.getMemberId(),
                String.valueOf(schedule.getId()),
                schedule.getDelayCompensationAmount(),
                schedule.getTrainScheduleId(),
                schedule.getId(),
                schedule.getDelayMinutes(),
                schedule.getDelayCompensationRate(),
                balanceBeforeCompensation
        );
        
        // 완료 상태로 변경
        saveSchedulePort.updateWithTransactionAtomically(
                schedule.getId(),
                MileageEarningSchedule.EarningStatus.COMPENSATION_PROCESSING,
                MileageEarningSchedule.EarningStatus.FULLY_COMPLETED,
                compensationTransaction.getId(),
                true
        );
        
        // 지연 보상 이벤트 발행
        eventPort.publishDelayCompensationEarnedEvent(
                compensationTransaction.getId(),
                compensationTransaction.getMemberId(),
                compensationTransaction.getPointsAmount().toString(),
                schedule.getDelayMinutes()
        );
    }
    
    /**
     * 스케줄 처리 실패 시 처리
     */
    private void handleScheduleFailure(MileageEarningSchedule schedule, Exception e) {
        log.error("마일리지 적립 스케줄 처리 실패 - 스케줄ID: {}", schedule.getId(), e);
        
        try {
            saveSchedulePort.updateStatusAtomically(
                    schedule.getId(),
                    MileageEarningSchedule.EarningStatus.BASE_PROCESSING,
                    MileageEarningSchedule.EarningStatus.FAILED
            );
            
            // 실패 정보 저장을 위해 엔티티 업데이트
            schedule.fail(e.getMessage());
            saveSchedulePort.save(schedule);
            
            // 실패 이벤트 발행
            eventPort.publishMileageEarningFailedEvent(
                    schedule.getId(),
                    schedule.getMemberId(),
                    e.getMessage()
            );
        } catch (Exception updateException) {
            log.error("실패 상태 업데이트 중 오류 발생 - 스케줄ID: {}", schedule.getId(), updateException);
        }
    }
    
    @Override
    @Transactional
    public CleanupResult cleanupOldCompletedSchedules(CleanupOldSchedulesCommand command) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(command.retentionDays());
        
        log.info("완료된 마일리지 적립 스케줄 정리 시작 - 보관기간: {}일, 기준시간: {}", 
                command.retentionDays(), cutoffTime);
        
        int deletedCount = loadSchedulePort.deleteCompletedSchedulesBeforeTime(cutoffTime);
        
        log.info("완료된 마일리지 적립 스케줄 정리 완료 - 삭제된 스케줄 수: {}", deletedCount);
        
        return new CleanupResult(
                deletedCount,
                String.format("%d개의 오래된 마일리지 적립 스케줄이 정리되었습니다.", deletedCount)
        );
    }
}