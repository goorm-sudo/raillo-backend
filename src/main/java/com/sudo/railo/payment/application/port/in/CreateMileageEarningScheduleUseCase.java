package com.sudo.railo.payment.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 마일리지 적립 스케줄 생성 유스케이스
 * 
 * 헥사고날 아키텍처의 입력 포트로, 결제 완료 시 마일리지 적립 스케줄을 생성하는 
 * 비즈니스 기능을 정의합니다.
 */
public interface CreateMileageEarningScheduleUseCase {
    
    /**
     * 마일리지 적립 스케줄 생성 명령
     */
    record CreateScheduleCommand(
        Long trainScheduleId,
        String paymentId,
        Long memberId,
        BigDecimal paymentAmount,
        LocalDateTime expectedArrivalTime
    ) {}
    
    /**
     * 마일리지 적립 스케줄 생성 결과
     */
    record ScheduleCreatedResult(
        Long scheduleId,
        BigDecimal baseMileageAmount,
        LocalDateTime scheduledEarningTime,
        String status
    ) {}
    
    /**
     * 결제 완료 시 마일리지 적립 스케줄을 생성합니다.
     * 
     * @param command 스케줄 생성 명령
     * @return 생성된 스케줄 정보
     */
    ScheduleCreatedResult createEarningSchedule(CreateScheduleCommand command);
}