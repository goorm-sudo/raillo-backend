package com.sudo.railo.payment.application.port.in;

/**
 * 마일리지 적립 처리 유스케이스
 * 
 * 헥사고날 아키텍처의 입력 포트로, 준비된 마일리지 적립 스케줄을 
 * 실제로 처리하는 비즈니스 기능을 정의합니다.
 */
public interface ProcessMileageEarningUseCase {
    
    /**
     * 배치 처리 명령
     */
    record ProcessBatchCommand(
        int batchSize
    ) {
        public ProcessBatchCommand() {
            this(100); // 기본 배치 크기
        }
    }
    
    /**
     * 개별 스케줄 처리 명령
     */
    record ProcessScheduleCommand(
        Long scheduleId
    ) {}
    
    /**
     * 배치 처리 결과
     */
    record BatchProcessedResult(
        int totalSchedules,
        int successCount,
        int failureCount
    ) {}
    
    /**
     * 준비된 마일리지 적립 스케줄들을 배치로 처리합니다.
     * 
     * @param command 배치 처리 명령
     * @return 처리 결과
     */
    BatchProcessedResult processReadySchedules(ProcessBatchCommand command);
    
    /**
     * 특정 마일리지 적립 스케줄을 처리합니다.
     * 
     * @param command 스케줄 처리 명령
     */
    void processEarningSchedule(ProcessScheduleCommand command);
}