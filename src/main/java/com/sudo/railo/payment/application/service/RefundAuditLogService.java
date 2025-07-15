package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.domain.entity.RefundAuditLog;
import com.sudo.railo.payment.domain.entity.RefundAuditLog.AuditEventType;
import com.sudo.railo.payment.infrastructure.persistence.RefundAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 환불 감사 로그 서비스
 * 
 * 토스 스타일로 중요한 이벤트만 선택적으로 저장합니다.
 * REQUIRES_NEW로 메인 트랜잭션과 독립적으로 동작합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundAuditLogService {
    
    private final RefundAuditLogRepository auditLogRepository;
    
    /**
     * 도착 후 환불 거부 기록
     * 가장 중요한 이벤트이므로 반드시 기록
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRefundDeniedAfterArrival(Long paymentId, Long reservationId, Long memberId, String detail) {
        try {
            RefundAuditLog auditLog = RefundAuditLog.createDeniedLog(
                paymentId,
                reservationId,
                memberId,
                AuditEventType.REFUND_DENIED_AFTER_ARRIVAL,
                "열차 도착 후 환불 시도",
                detail
            );
            
            auditLogRepository.save(auditLog);
            log.debug("환불 거부 감사 로그 저장 - paymentId: {}, type: {}", paymentId, AuditEventType.REFUND_DENIED_AFTER_ARRIVAL);
            
        } catch (Exception e) {
            // 감사 로그 실패가 메인 프로세스를 방해하지 않음
            log.error("감사 로그 저장 실패 - paymentId: {}", paymentId, e);
        }
    }
    
    /**
     * 중복 환불 시도 기록
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDuplicateRefundAttempt(Long paymentId, Long existingRefundId) {
        try {
            RefundAuditLog auditLog = RefundAuditLog.builder()
                .paymentId(paymentId)
                .eventType(AuditEventType.REFUND_DENIED_DUPLICATE)
                .eventReason("중복 환불 시도")
                .eventDetail(String.format("기존 환불 ID: %d", existingRefundId))
                .build();
                
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("중복 환불 감사 로그 저장 실패", e);
        }
    }
    
    /**
     * Unknown 상태 발생 기록
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUnknownState(Long paymentId, String errorDetail) {
        try {
            RefundAuditLog auditLog = RefundAuditLog.builder()
                .paymentId(paymentId)
                .eventType(AuditEventType.REFUND_UNKNOWN_STATE)
                .eventReason("PG사 통신 오류")
                .eventDetail(errorDetail)
                .build();
                
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Unknown 상태 감사 로그 저장 실패", e);
        }
    }
    
    /**
     * 재시도 성공 기록
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRetrySuccess(Long paymentId, Long refundCalculationId, int attemptCount) {
        try {
            RefundAuditLog auditLog = RefundAuditLog.builder()
                .paymentId(paymentId)
                .eventType(AuditEventType.REFUND_RETRY_SUCCESS)
                .eventReason(String.format("%d번째 재시도 성공", attemptCount))
                .eventDetail(String.format("refundCalculationId: %d", refundCalculationId))
                .build();
                
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("재시도 성공 감사 로그 저장 실패", e);
        }
    }
    
    /**
     * 특정 기간의 감사 로그 조회 (운영팀용)
     */
    @Transactional(readOnly = true)
    public List<RefundAuditLog> getAuditLogs(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startTime, endTime);
    }
    
    /**
     * 특정 결제의 감사 로그 조회
     */
    @Transactional(readOnly = true)
    public List<RefundAuditLog> getAuditLogsByPaymentId(Long paymentId) {
        return auditLogRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }
}