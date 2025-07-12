package com.sudo.railo.payment.application.port.in;

/**
 * 마일리지 적립 관리 유스케이스
 * 
 * 헥사고날 아키텍처의 입력 포트로, 마일리지 적립 시스템의 
 * 관리 및 정리 작업을 정의합니다.
 */
public interface ManageMileageEarningUseCase {
    
    /**
     * 오래된 완료 스케줄 정리 명령
     */
    record CleanupOldSchedulesCommand(
        int retentionDays
    ) {
        public CleanupOldSchedulesCommand() {
            this(90); // 기본 보관 기간 90일
        }
    }
    
    /**
     * 정리 작업 결과
     */
    record CleanupResult(
        int deletedCount,
        String message
    ) {}
    
    /**
     * 오래된 완료된 마일리지 적립 스케줄을 정리합니다.
     * 
     * @param command 정리 작업 명령
     * @return 정리 결과
     */
    CleanupResult cleanupOldCompletedSchedules(CleanupOldSchedulesCommand command);
}