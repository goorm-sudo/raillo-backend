package com.sudo.railo.payment.application.event;

import com.sudo.railo.payment.application.port.in.CreateMileageEarningScheduleUseCase;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * 마일리지 적립 스케줄 생성 이벤트 리스너
 * 결제 완료 시 열차 도착 시점에 마일리지가 적립되도록 스케줄을 생성
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MileageScheduleEventListener {
    
    private final CreateMileageEarningScheduleUseCase createMileageEarningScheduleUseCase;
    private final PaymentRepository paymentRepository;
    
    /**
     * 결제 상태 변경 이벤트 처리 - SUCCESS 상태일 때 마일리지 적립 스케줄 생성
     * 
     * @param event 결제 상태 변경 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    public void handlePaymentStateChanged(PaymentStateChangedEvent event) {
        // SUCCESS 상태가 아니면 처리하지 않음
        if (event.getNewStatus() != PaymentExecutionStatus.SUCCESS) {
            return;
        }
        
        log.info("마일리지 적립 스케줄 생성 시작 - 결제ID: {}, 예약ID: {}", 
                event.getPaymentId(), event.getReservationId());
        
        try {
            // Payment 엔티티 조회
            Payment payment = paymentRepository.findById(Long.parseLong(event.getPaymentId()))
                    .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + event.getPaymentId()));
            
            // 회원이 아니면 마일리지 적립 스케줄 생성하지 않음
            Long memberId = payment.getMember() != null ? payment.getMember().getId() : null;
            if (memberId == null) {
                log.debug("비회원 결제는 마일리지 적립 스케줄을 생성하지 않습니다 - 결제ID: {}", payment.getId());
                return;
            }
            
            // Payment에 저장된 열차 정보 사용 (예약이 삭제되어도 처리 가능)
            Long trainScheduleId = payment.getTrainScheduleId();
            LocalDateTime expectedArrivalTime = payment.getTrainArrivalTime();
            
            // 열차 정보가 없는 경우 처리
            if (trainScheduleId == null || expectedArrivalTime == null) {
                log.error("결제에 열차 정보가 없습니다. 마일리지 적립 스케줄을 생성할 수 없습니다 - 결제ID: {}", payment.getId());
                
                // 알림 또는 수동 처리를 위한 로직 추가 가능
                // 예: 관리자에게 알림 발송, 별도 처리 큐에 저장 등
                
                return;
            }
            
            // 마일리지 적립 스케줄 생성
            CreateMileageEarningScheduleUseCase.CreateScheduleCommand command = 
                new CreateMileageEarningScheduleUseCase.CreateScheduleCommand(
                    trainScheduleId,
                    payment.getId().toString(),
                    memberId,
                    payment.getAmountPaid(),
                    expectedArrivalTime
                );
            
            createMileageEarningScheduleUseCase.createEarningSchedule(command);
            
            log.info("마일리지 적립 스케줄 생성 완료 - 결제ID: {}, 열차스케줄ID: {}, 예상도착시간: {}", 
                    payment.getId(), trainScheduleId, expectedArrivalTime);
            
        } catch (Exception e) {
            log.error("마일리지 적립 스케줄 생성 중 오류 발생 - 결제ID: {}", event.getPaymentId(), e);
            // 스케줄 생성 실패는 메인 결제 트랜잭션에 영향주지 않음
            // 필요시 재시도 로직이나 수동 처리를 위한 알림 추가 가능
        }
    }
} 