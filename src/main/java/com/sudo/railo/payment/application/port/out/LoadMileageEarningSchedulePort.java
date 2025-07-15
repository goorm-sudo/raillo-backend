package com.sudo.railo.payment.application.port.out;

import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 마일리지 적립 스케줄 조회 포트
 * 
 * 헥사고날 아키텍처의 출력 포트로, 영속성 계층에서 
 * 마일리지 적립 스케줄을 조회하는 기능을 정의합니다.
 */
public interface LoadMileageEarningSchedulePort {
    
    /**
     * ID로 스케줄을 조회합니다.
     */
    Optional<MileageEarningSchedule> findById(Long scheduleId);
    
    /**
     * 처리 준비된 스케줄을 비관적 락과 함께 조회합니다.
     * FOR UPDATE SKIP LOCKED를 사용하여 동시성 문제를 방지합니다.
     */
    List<MileageEarningSchedule> findReadySchedulesWithLock(LocalDateTime currentTime, int limit);
    
    /**
     * 열차 스케줄 ID로 적립 스케줄들을 조회합니다.
     */
    List<MileageEarningSchedule> findByTrainScheduleId(Long trainScheduleId);
    
    /**
     * 결제 ID로 적립 스케줄을 조회합니다.
     */
    Optional<MileageEarningSchedule> findByPaymentId(String paymentId);
    
    /**
     * 회원 ID로 적립 스케줄들을 조회합니다.
     */
    List<MileageEarningSchedule> findByMemberId(Long memberId);
    
    /**
     * 회원 ID와 상태로 적립 스케줄들을 조회합니다.
     */
    List<MileageEarningSchedule> findByMemberIdAndStatus(
        Long memberId, 
        MileageEarningSchedule.EarningStatus status
    );
    
    /**
     * 회원의 적립 예정 마일리지 총액을 계산합니다.
     */
    BigDecimal calculatePendingMileageByMemberId(Long memberId);
    
    /**
     * 마일리지 적립 통계를 조회합니다.
     */
    Object getMileageEarningStatistics(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 지연 보상 통계를 조회합니다.
     */
    Object getDelayCompensationStatistics(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 특정 시간 이전에 완료된 스케줄들을 삭제합니다.
     * 
     * @param cutoffTime 삭제 기준 시간
     * @return 삭제된 스케줄 수
     */
    int deleteCompletedSchedulesBeforeTime(LocalDateTime cutoffTime);
}