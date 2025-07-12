package com.sudo.railo.payment.application.port.out;

import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 마일리지 적립 스케줄 저장 포트
 * 
 * 헥사고날 아키텍처의 출력 포트로, 영속성 계층에서
 * 마일리지 적립 스케줄을 저장하고 업데이트하는 기능을 정의합니다.
 */
public interface SaveMileageEarningSchedulePort {
    
    /**
     * 마일리지 적립 스케줄을 저장합니다.
     */
    MileageEarningSchedule save(MileageEarningSchedule schedule);
    
    /**
     * 스케줄 상태를 원자적으로 변경합니다.
     * 예상 상태일 때만 새로운 상태로 변경하여 동시성 문제를 방지합니다.
     * 
     * @return 업데이트된 행 수 (0이면 이미 다른 프로세스가 처리)
     */
    int updateStatusAtomically(
        Long scheduleId,
        MileageEarningSchedule.EarningStatus expectedStatus,
        MileageEarningSchedule.EarningStatus newStatus
    );
    
    /**
     * 스케줄 처리 완료 시 트랜잭션 정보와 함께 원자적으로 업데이트합니다.
     */
    int updateWithTransactionAtomically(
        Long scheduleId,
        MileageEarningSchedule.EarningStatus expectedStatus,
        MileageEarningSchedule.EarningStatus newStatus,
        Long transactionId,
        boolean isFullyCompleted
    );
    
    /**
     * 여러 스케줄을 한 번에 저장합니다.
     */
    List<MileageEarningSchedule> saveAll(List<MileageEarningSchedule> schedules);
    
    /**
     * 오래된 완료 스케줄을 삭제합니다.
     */
    int deleteCompletedSchedulesBeforeTime(LocalDateTime beforeTime);
}