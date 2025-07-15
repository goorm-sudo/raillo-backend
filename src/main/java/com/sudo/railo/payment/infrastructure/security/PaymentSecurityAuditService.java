package com.sudo.railo.payment.infrastructure.security;

import com.sudo.railo.global.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 결제 정보 보안 감사 서비스
 * 
 * 민감한 결제 정보에 대한 접근 및 처리를 감사하고 기록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSecurityAuditService {

    /**
     * 복호화 시도 로그 기록
     */
    public void logDecryptionAttempt() {
        String username = getCurrentUser();
        log.info("Payment data decryption attempted - User: {}, Time: {}", 
            username, LocalDateTime.now());
    }

    /**
     * 복호화 성공 로그 기록
     */
    public void logDecryptionSuccess() {
        String username = getCurrentUser();
        log.info("Payment data decryption successful - User: {}, Time: {}", 
            username, LocalDateTime.now());
    }

    /**
     * 복호화 실패 로그 기록
     */
    public void logDecryptionFailure(String reason) {
        String username = getCurrentUser();
        log.error("Payment data decryption failed - User: {}, Reason: {}, Time: {}", 
            username, reason, LocalDateTime.now());
    }

    /**
     * 결제수단 저장 로그 기록
     */
    public void logPaymentMethodSaved(Long memberId, String paymentType) {
        String username = getCurrentUser();
        log.info("Payment method saved - User: {}, MemberId: {}, Type: {}, Time: {}", 
            username, memberId, paymentType, LocalDateTime.now());
    }

    /**
     * 결제수단 삭제 로그 기록
     */
    public void logPaymentMethodDeleted(Long paymentMethodId) {
        String username = getCurrentUser();
        log.info("Payment method deleted - User: {}, PaymentMethodId: {}, Time: {}", 
            username, paymentMethodId, LocalDateTime.now());
    }

    /**
     * 민감정보 조회 로그 기록
     */
    public void logSensitiveDataAccess(String dataType, String purpose) {
        String username = getCurrentUser();
        log.info("Sensitive data accessed - User: {}, DataType: {}, Purpose: {}, Time: {}", 
            username, dataType, purpose, LocalDateTime.now());
    }

    /**
     * 보안 정책 위반 로그 기록
     */
    public void logSecurityViolation(String violationType, String details) {
        String username = getCurrentUser();
        log.error("Security violation detected - User: {}, Type: {}, Details: {}, Time: {}", 
            username, violationType, details, LocalDateTime.now());
    }

    /**
     * 데이터 마이그레이션 로그 기록
     */
    public void logDataMigration(String migrationType, int recordCount) {
        String username = getCurrentUser();
        log.info("Data migration performed - User: {}, Type: {}, Records: {}, Time: {}", 
            username, migrationType, recordCount, LocalDateTime.now());
    }
    
    /**
     * 현재 사용자 정보 조회
     */
    private String getCurrentUser() {
        try {
            return SecurityUtil.getCurrentMemberNo();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}