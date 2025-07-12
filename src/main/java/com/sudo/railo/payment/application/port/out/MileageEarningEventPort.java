package com.sudo.railo.payment.application.port.out;

import java.math.BigDecimal;

/**
 * 마일리지 적립 이벤트 발행 포트
 * 
 * 헥사고날 아키텍처의 출력 포트로, 마일리지 적립 관련 
 * 도메인 이벤트를 발행하는 기능을 정의합니다.
 */
public interface MileageEarningEventPort {
    
    /**
     * 마일리지 적립 준비 완료 이벤트를 발행합니다.
     */
    void publishMileageEarningReadyEvent(
        Long scheduleId,
        Long memberId,
        String aggregateId
    );
    
    /**
     * 기본 마일리지 적립 완료 이벤트를 발행합니다.
     */
    void publishMileageEarnedEvent(
        Long transactionId,
        Long memberId,
        String pointsAmount,
        String earningType
    );
    
    /**
     * 지연 보상 마일리지 적립 완료 이벤트를 발행합니다.
     */
    void publishDelayCompensationEarnedEvent(
        Long transactionId,
        Long memberId,
        String compensationAmount,
        int delayMinutes
    );
    
    /**
     * 마일리지 적립 실패 이벤트를 발행합니다.
     */
    void publishMileageEarningFailedEvent(
        Long scheduleId,
        Long memberId,
        String reason
    );
}