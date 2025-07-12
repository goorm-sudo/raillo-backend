package com.sudo.railo.payment.infrastructure.adapter.out.persistence;

import com.sudo.railo.payment.application.port.out.LoadMileageEarningSchedulePort;
import com.sudo.railo.payment.application.port.out.SaveMileageEarningSchedulePort;
import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import com.sudo.railo.payment.domain.repository.MileageEarningScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 마일리지 적립 스케줄 영속성 어댑터
 * 
 * 헥사고날 아키텍처의 어댑터로, 출력 포트를 구현하여
 * 실제 데이터베이스 접근을 담당합니다.
 * 동시성 제어를 위한 비관적 락과 원자적 업데이트를 지원합니다.
 */
@Component
@RequiredArgsConstructor
public class MileageEarningSchedulePersistenceAdapter 
        implements LoadMileageEarningSchedulePort, SaveMileageEarningSchedulePort {
    
    private final MileageEarningScheduleRepository repository;
    
    // LoadMileageEarningSchedulePort 구현
    
    @Override
    public Optional<MileageEarningSchedule> findById(Long scheduleId) {
        return repository.findById(scheduleId);
    }
    
    @Override
    public List<MileageEarningSchedule> findReadySchedulesWithLock(LocalDateTime currentTime, int limit) {
        // FOR UPDATE SKIP LOCKED를 사용하여 동시성 문제 방지
        return repository.findReadySchedulesWithLockAndLimit(currentTime, limit);
    }
    
    @Override
    public List<MileageEarningSchedule> findByTrainScheduleId(Long trainScheduleId) {
        return repository.findByTrainScheduleId(trainScheduleId);
    }
    
    @Override
    public Optional<MileageEarningSchedule> findByPaymentId(String paymentId) {
        return repository.findByPaymentId(paymentId);
    }
    
    @Override
    public List<MileageEarningSchedule> findByMemberId(Long memberId) {
        return repository.findByMemberId(memberId);
    }
    
    @Override
    public List<MileageEarningSchedule> findByMemberIdAndStatus(
            Long memberId, MileageEarningSchedule.EarningStatus status) {
        return repository.findByMemberIdAndStatus(memberId, status);
    }
    
    @Override
    public BigDecimal calculatePendingMileageByMemberId(Long memberId) {
        return repository.calculatePendingMileageByMemberId(memberId);
    }
    
    @Override
    public Object getMileageEarningStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return repository.getMileageEarningStatistics(startTime, endTime);
    }
    
    @Override
    public Object getDelayCompensationStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return repository.getDelayCompensationStatistics(startTime, endTime);
    }
    
    // SaveMileageEarningSchedulePort 구현
    
    @Override
    public MileageEarningSchedule save(MileageEarningSchedule schedule) {
        return repository.save(schedule);
    }
    
    @Override
    public int updateStatusAtomically(
            Long scheduleId,
            MileageEarningSchedule.EarningStatus expectedStatus,
            MileageEarningSchedule.EarningStatus newStatus) {
        // 원자적 상태 변경으로 동시성 문제 방지
        return repository.updateStatusAtomically(scheduleId, expectedStatus, newStatus);
    }
    
    @Override
    public int updateWithTransactionAtomically(
            Long scheduleId,
            MileageEarningSchedule.EarningStatus expectedStatus,
            MileageEarningSchedule.EarningStatus newStatus,
            Long transactionId,
            boolean isFullyCompleted) {
        return repository.updateWithTransactionAtomically(
            scheduleId, expectedStatus, newStatus, transactionId, isFullyCompleted
        );
    }
    
    @Override
    public List<MileageEarningSchedule> saveAll(List<MileageEarningSchedule> schedules) {
        return repository.saveAll(schedules);
    }
    
    @Override
    public int deleteCompletedSchedulesBeforeTime(LocalDateTime beforeTime) {
        return repository.deleteCompletedSchedulesBeforeTime(beforeTime);
    }
}