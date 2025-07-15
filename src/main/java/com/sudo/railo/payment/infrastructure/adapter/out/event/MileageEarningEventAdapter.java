package com.sudo.railo.payment.infrastructure.adapter.out.event;

import com.sudo.railo.payment.application.port.out.MileageEarningEventPort;
import com.sudo.railo.payment.application.service.DomainEventOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 마일리지 적립 이벤트 어댑터
 * 
 * 헥사고날 아키텍처의 어댑터로, 마일리지 적립 관련
 * 도메인 이벤트 발행을 담당합니다.
 */
@Component
@RequiredArgsConstructor
public class MileageEarningEventAdapter implements MileageEarningEventPort {
    
    private final DomainEventOutboxService domainEventOutboxService;
    
    @Override
    public void publishMileageEarningReadyEvent(
            Long scheduleId,
            Long memberId,
            String aggregateId) {
        
        domainEventOutboxService.publishMileageEarningReadyEvent(
            scheduleId, memberId, aggregateId
        );
    }
    
    @Override
    public void publishMileageEarnedEvent(
            Long transactionId,
            Long memberId,
            String pointsAmount,
            String earningType) {
        
        domainEventOutboxService.publishMileageEarnedEvent(
            transactionId, memberId, pointsAmount, earningType
        );
    }
    
    @Override
    public void publishDelayCompensationEarnedEvent(
            Long transactionId,
            Long memberId,
            String compensationAmount,
            int delayMinutes) {
        
        domainEventOutboxService.publishDelayCompensationEarnedEvent(
            transactionId, memberId, compensationAmount, delayMinutes
        );
    }
    
    @Override
    public void publishMileageEarningFailedEvent(
            Long scheduleId,
            Long memberId,
            String reason) {
        
        // DomainEventOutboxService에 실패 이벤트 메서드가 없으므로
        // 일반적인 이벤트로 발행하거나 새로운 메서드 추가 필요
        // TODO: 실패 이벤트 발행 메서드 구현
        domainEventOutboxService.publishMileageEarningReadyEvent(
            scheduleId, memberId, "FAILED:" + reason
        );
    }
}