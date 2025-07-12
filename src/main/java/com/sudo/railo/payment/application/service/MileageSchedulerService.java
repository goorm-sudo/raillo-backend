package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.payment.domain.repository.MileageEarningScheduleRepository;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.domain.service.MileageExecutionService;
import com.sudo.railo.train.application.TrainScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 마일리지 적립 스케줄러 서비스
 * 열차 도착 시점에 마일리지를 적립하는 배치 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MileageSchedulerService {
    
    private final MileageEarningScheduleRepository scheduleRepository;
    private final PaymentRepository paymentRepository;
    private final MileageExecutionService mileageExecutionService;
    private final TrainScheduleService trainScheduleService;
    
    /**
     * 5분마다 실행되는 스케줄러
     * 도착 시간이 지난 SCHEDULED 상태의 스케줄을 READY로 변경
     */
    @Scheduled(fixedDelay = 300000) // 5분
    @Transactional
    public void updateReadySchedules() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("마일리지 적립 스케줄 상태 업데이트 시작 - 현재시간: {}", now);
        
        List<MileageEarningSchedule> scheduledList = scheduleRepository.findScheduledBeforeTime(now);
        
        if (!scheduledList.isEmpty()) {
            log.info("READY 상태로 변경할 스케줄 수: {}", scheduledList.size());
            
            scheduledList.forEach(schedule -> {
                schedule.markReady();
                log.debug("스케줄 READY 변경 - scheduleId: {}, trainScheduleId: {}, memberId: {}", 
                    schedule.getId(), schedule.getTrainScheduleId(), schedule.getMemberId());
            });
            
            scheduleRepository.saveAll(scheduledList);
        }
    }
    
    /**
     * 5분마다 실행되는 스케줄러
     * READY 상태의 스케줄에 대해 기본 마일리지 적립 처리
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5분 간격, 1분 후 시작
    public void processMileageEarning() {
        log.debug("마일리지 적립 처리 시작");
        
        List<MileageEarningSchedule> readySchedules = scheduleRepository.findReadySchedules();
        
        if (readySchedules.isEmpty()) {
            return;
        }
        
        log.info("처리할 READY 스케줄 수: {}", readySchedules.size());
        
        for (MileageEarningSchedule schedule : readySchedules) {
            try {
                processIndividualSchedule(schedule);
            } catch (Exception e) {
                log.error("마일리지 적립 처리 실패 - scheduleId: {}", schedule.getId(), e);
            }
        }
    }
    
    /**
     * 개별 스케줄 처리
     */
    @Transactional
    protected void processIndividualSchedule(MileageEarningSchedule schedule) {
        try {
            // 결제 정보 조회
            Payment payment = paymentRepository.findById(Long.valueOf(schedule.getPaymentId()))
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다: " + schedule.getPaymentId()));
            
            // 환불된 결제는 처리하지 않음
            if (payment.getPaymentStatus() == PaymentExecutionStatus.REFUNDED ||
                payment.getPaymentStatus() == PaymentExecutionStatus.CANCELLED) {
                schedule.setStatus(MileageEarningSchedule.EarningStatus.CANCELLED);
                schedule.setErrorMessage("환불된 결제로 인한 적립 취소");
                schedule.setProcessedAt(LocalDateTime.now());
                scheduleRepository.save(schedule);
                log.info("환불된 결제의 마일리지 적립 취소 - paymentId: {}", payment.getId());
                return;
            }
            
            // 기본 마일리지 적립
            schedule.startBaseProcessing();
            scheduleRepository.save(schedule);
            
            MileageTransaction transaction = mileageExecutionService.executeEarning(payment);
            
            if (transaction != null) {
                schedule.completeBaseEarning(transaction.getId());
                
                // 지연 정보 확인 및 보상 계산
                checkAndUpdateDelayCompensation(schedule);
                
                scheduleRepository.save(schedule);
                log.info("기본 마일리지 적립 완료 - scheduleId: {}, transactionId: {}, amount: {}", 
                    schedule.getId(), transaction.getId(), schedule.getBaseMileageAmount());
            }
            
        } catch (Exception e) {
            schedule.fail(e.getMessage());
            scheduleRepository.save(schedule);
            throw new RuntimeException("마일리지 적립 처리 실패", e);
        }
    }
    
    /**
     * 5분마다 실행되는 스케줄러
     * 지연 보상 마일리지 처리
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 120000) // 5분 간격, 2분 후 시작
    @Transactional
    public void processDelayCompensation() {
        log.debug("지연 보상 마일리지 처리 시작");
        
        List<MileageEarningSchedule> compensationSchedules = 
            scheduleRepository.findBaseCompletedWithCompensation();
        
        if (compensationSchedules.isEmpty()) {
            return;
        }
        
        log.info("처리할 지연 보상 스케줄 수: {}", compensationSchedules.size());
        
        for (MileageEarningSchedule schedule : compensationSchedules) {
            try {
                processDelayCompensationForSchedule(schedule);
            } catch (Exception e) {
                log.error("지연 보상 처리 실패 - scheduleId: {}", schedule.getId(), e);
            }
        }
    }
    
    /**
     * 개별 지연 보상 처리
     */
    @Transactional
    protected void processDelayCompensationForSchedule(MileageEarningSchedule schedule) {
        try {
            schedule.startCompensationProcessing();
            scheduleRepository.save(schedule);
            
            // 결제 정보 조회
            Payment payment = paymentRepository.findById(Long.valueOf(schedule.getPaymentId()))
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다"));
            
            // 지연 보상 마일리지 적립
            MileageTransaction compensation = mileageExecutionService.restoreMileageUsage(
                payment.getId().toString(),
                payment.getMemberId(),
                schedule.getDelayCompensationAmount(),
                String.format("열차 지연 보상 마일리지 (%d분 지연, %s)", 
                    schedule.getDelayMinutes(), schedule.getRouteInfo())
            );
            
            schedule.completeCompensationEarning(compensation.getId());
            scheduleRepository.save(schedule);
            
            log.info("지연 보상 마일리지 적립 완료 - scheduleId: {}, amount: {}, delayMinutes: {}", 
                schedule.getId(), schedule.getDelayCompensationAmount(), schedule.getDelayMinutes());
                
        } catch (Exception e) {
            schedule.fail("지연 보상 처리 실패: " + e.getMessage());
            scheduleRepository.save(schedule);
            throw new RuntimeException("지연 보상 처리 실패", e);
        }
    }
    
    /**
     * 지연 시간에 따른 보상율 계산
     * 20-40분: 12.5%, 40-60분: 25%, 60분 이상: 50%
     */
    public BigDecimal calculateCompensationRate(int delayMinutes) {
        if (delayMinutes < 20) {
            return BigDecimal.ZERO;
        } else if (delayMinutes < 40) {
            return new BigDecimal("0.125");
        } else if (delayMinutes < 60) {
            return new BigDecimal("0.25");
        } else {
            return new BigDecimal("0.5");
        }
    }
    
    /**
     * 지연 정보 확인 및 보상 업데이트
     */
    private void checkAndUpdateDelayCompensation(MileageEarningSchedule schedule) {
        try {
            // TrainScheduleService에서 실시간 지연 정보 조회
            TrainScheduleService.TrainTimeInfo timeInfo = 
                trainScheduleService.getTrainTimeInfo(schedule.getTrainScheduleId());
            
            if (timeInfo != null && timeInfo.delayMinutes() != null && timeInfo.delayMinutes() > 0) {
                int delayMinutes = timeInfo.delayMinutes();
                BigDecimal compensationRate = calculateCompensationRate(delayMinutes);
                
                if (compensationRate.compareTo(BigDecimal.ZERO) > 0) {
                    // 지연 보상 정보 업데이트
                    schedule.updateDelayInfo(delayMinutes, compensationRate);
                    log.info("지연 보상 정보 업데이트 - scheduleId: {}, delayMinutes: {}, compensationRate: {}%", 
                        schedule.getId(), delayMinutes, compensationRate.multiply(new BigDecimal("100")));
                }
            }
        } catch (Exception e) {
            log.error("지연 정보 조회 실패 - scheduleId: {}", schedule.getId(), e);
            // 지연 정보 조회 실패는 기본 적립을 막지 않음
        }
    }
}