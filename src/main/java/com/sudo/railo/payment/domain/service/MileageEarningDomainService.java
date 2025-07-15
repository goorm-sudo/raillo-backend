package com.sudo.railo.payment.domain.service;

import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import com.sudo.railo.payment.domain.util.DelayCompensationCalculator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * 마일리지 적립 도메인 서비스
 * 
 * 순수한 도메인 로직만을 포함하며, 프레임워크나 인프라스트럭처에
 * 의존하지 않는 비즈니스 규칙을 구현합니다.
 */
@Service
public class MileageEarningDomainService {
    
    // 마일리지 적립 비율 (결제 금액의 1%)
    private static final BigDecimal EARNING_RATE = new BigDecimal("0.01");
    
    /**
     * 정상 운행 기준 마일리지 적립 스케줄을 생성합니다.
     * 
     * @param trainScheduleId 열차 스케줄 ID
     * @param paymentId 결제 ID
     * @param memberId 회원 ID
     * @param paymentAmount 결제 금액
     * @param expectedArrivalTime 예상 도착 시간
     * @param routeInfo 노선 정보
     * @return 생성된 마일리지 적립 스케줄
     */
    public MileageEarningSchedule createNormalEarningSchedule(
            Long trainScheduleId,
            String paymentId,
            Long memberId,
            BigDecimal paymentAmount,
            LocalDateTime expectedArrivalTime,
            String routeInfo) {
        
        return MileageEarningSchedule.createNormalEarningSchedule(
            trainScheduleId,
            paymentId,
            memberId,
            paymentAmount,
            expectedArrivalTime,
            routeInfo
        );
    }
    
    /**
     * 기본 마일리지 금액을 계산합니다.
     * 
     * @param paymentAmount 결제 금액
     * @return 기본 마일리지 금액
     */
    public BigDecimal calculateBaseMileage(BigDecimal paymentAmount) {
        return paymentAmount.multiply(EARNING_RATE).setScale(0, BigDecimal.ROUND_DOWN);
    }
    
    /**
     * 지연 보상 마일리지를 계산합니다.
     * 
     * @param originalAmount 원본 결제 금액 (운임)
     * @param delayMinutes 지연 시간(분)
     * @return 지연 보상 마일리지 금액
     */
    public BigDecimal calculateDelayCompensation(BigDecimal originalAmount, int delayMinutes) {
        BigDecimal compensationRate = DelayCompensationCalculator.calculateCompensationRate(delayMinutes);
        return originalAmount.multiply(compensationRate).setScale(0, BigDecimal.ROUND_DOWN);
    }
    
    /**
     * 스케줄이 처리 가능한 상태인지 검증합니다.
     * 
     * @param schedule 마일리지 적립 스케줄
     * @return 처리 가능 여부
     */
    public boolean isProcessable(MileageEarningSchedule schedule) {
        return schedule.getStatus() == MileageEarningSchedule.EarningStatus.READY
            && schedule.getScheduledEarningTime().isBefore(LocalDateTime.now());
    }
    
    /**
     * 스케줄 상태 전이가 유효한지 검증합니다.
     * 
     * @param currentStatus 현재 상태
     * @param newStatus 변경하려는 상태
     * @return 전이 가능 여부
     */
    public boolean isValidStatusTransition(
            MileageEarningSchedule.EarningStatus currentStatus,
            MileageEarningSchedule.EarningStatus newStatus) {
        
        return switch (currentStatus) {
            case SCHEDULED -> newStatus == MileageEarningSchedule.EarningStatus.READY
                || newStatus == MileageEarningSchedule.EarningStatus.CANCELLED;
            
            case READY -> newStatus == MileageEarningSchedule.EarningStatus.BASE_PROCESSING
                || newStatus == MileageEarningSchedule.EarningStatus.CANCELLED;
            
            case BASE_PROCESSING -> newStatus == MileageEarningSchedule.EarningStatus.BASE_COMPLETED
                || newStatus == MileageEarningSchedule.EarningStatus.FULLY_COMPLETED
                || newStatus == MileageEarningSchedule.EarningStatus.FAILED;
            
            case BASE_COMPLETED -> newStatus == MileageEarningSchedule.EarningStatus.COMPENSATION_PROCESSING;
            
            case COMPENSATION_PROCESSING -> newStatus == MileageEarningSchedule.EarningStatus.FULLY_COMPLETED
                || newStatus == MileageEarningSchedule.EarningStatus.FAILED;
            
            case FULLY_COMPLETED, CANCELLED, FAILED -> false; // 종료 상태에서는 변경 불가
        };
    }
    
    /**
     * 지연 보상이 필요한지 확인합니다.
     * 
     * @param delayMinutes 지연 시간(분)
     * @return 보상 필요 여부
     */
    public boolean requiresDelayCompensation(int delayMinutes) {
        return delayMinutes >= 20; // 20분 이상 지연 시 보상
    }
}