package com.sudo.railo.payment.infrastructure.persistence;

import com.sudo.railo.payment.domain.entity.RefundAuditLog;
import com.sudo.railo.payment.domain.entity.RefundAuditLog.AuditEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 환불 감사 로그 Repository
 */
@Repository
public interface RefundAuditLogRepository extends JpaRepository<RefundAuditLog, Long> {
    
    List<RefundAuditLog> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
    
    List<RefundAuditLog> findByEventTypeOrderByCreatedAtDesc(AuditEventType eventType);
    
    List<RefundAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime);
    
    List<RefundAuditLog> findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long memberId, LocalDateTime startTime, LocalDateTime endTime);
    
    Long countByEventTypeAndCreatedAtBetween(
            AuditEventType eventType, LocalDateTime startTime, LocalDateTime endTime);
}