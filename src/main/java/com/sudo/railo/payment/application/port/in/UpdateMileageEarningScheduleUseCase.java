package com.sudo.railo.payment.application.port.in;

import java.time.LocalDateTime;

/**
 * 마일리지 적립 스케줄 업데이트 유스케이스
 * 
 * 헥사고날 아키텍처의 입력 포트로, 열차 도착 및 지연 시 
 * 마일리지 적립 스케줄을 업데이트하는 비즈니스 기능을 정의합니다.
 */
public interface UpdateMileageEarningScheduleUseCase {
    
    /**
     * 열차 도착 시 스케줄 준비 명령
     */
    record MarkScheduleReadyCommand(
        Long trainScheduleId,
        LocalDateTime actualArrivalTime
    ) {}
    
    /**
     * 열차 지연 시 보상 업데이트 명령
     */
    record UpdateDelayCompensationCommand(
        Long trainScheduleId,
        int delayMinutes,
        LocalDateTime actualArrivalTime
    ) {}
    
    /**
     * 스케줄 업데이트 결과
     */
    record ScheduleUpdateResult(
        int affectedSchedules,
        String status
    ) {}
    
    /**
     * 열차 도착 시 마일리지 적립 스케줄을 READY 상태로 변경합니다.
     * 
     * @param command 스케줄 준비 명령
     * @return 업데이트 결과
     */
    ScheduleUpdateResult markScheduleReady(MarkScheduleReadyCommand command);
    
    /**
     * 열차 지연 시 지연 보상 마일리지를 계산하고 업데이트합니다.
     * 
     * @param command 지연 보상 업데이트 명령  
     * @return 업데이트 결과
     */
    ScheduleUpdateResult updateDelayCompensation(UpdateDelayCompensationCommand command);
}