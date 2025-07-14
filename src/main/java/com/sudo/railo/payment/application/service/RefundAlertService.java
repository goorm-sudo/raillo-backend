package com.sudo.railo.payment.application.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 환불 알림 서비스
 * 
 * 토스/당근 스타일의 실시간 모니터링과 알림을 제공합니다.
 * Slack, Discord, 이메일 등으로 알림을 보낼 수 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundAlertService {
    
    private final MeterRegistry meterRegistry;
    
    @Value("${alert.refund.failure-threshold:10}")
    private int failureThreshold;  // 10회 이상 실패시 알림
    
    @Value("${alert.refund.rate-threshold:0.1}")
    private double failureRateThreshold;  // 10% 이상 실패율시 알림
    
    // 시간대별 실패 카운터 (실무에서는 Redis 사용)
    private final ConcurrentHashMap<String, AtomicInteger> hourlyFailureCount = new ConcurrentHashMap<>();
    
    /**
     * 환불 실패 알림
     */
    @Async
    public void alertRefundFailure(Long paymentId, String reason, String errorDetail) {
        String currentHour = LocalDateTime.now().toString().substring(0, 13);
        AtomicInteger counter = hourlyFailureCount.computeIfAbsent(currentHour, k -> new AtomicInteger(0));
        int failureCount = counter.incrementAndGet();
        
        // 임계값 초과시 알림
        if (failureCount == failureThreshold) {
            sendUrgentAlert(String.format(
                "[긴급] 환불 실패 급증 - 현재 시간대 %d건 발생\n" +
                "최근 실패: paymentId=%d, 사유=%s",
                failureCount, paymentId, reason
            ));
        }
        
        // 개별 중요 실패 알림
        if (isImportantFailure(reason)) {
            sendAlert(String.format(
                "[환불 실패] paymentId: %d\n사유: %s\n상세: %s",
                paymentId, reason, errorDetail
            ));
        }
        
        log.warn("환불 실패 기록 - paymentId: {}, 사유: {}, 시간대 실패수: {}", 
                paymentId, reason, failureCount);
    }
    
    /**
     * 환불 거부 알림 (도착 후 환불 등)
     */
    @Async
    public void alertRefundDenied(Long paymentId, String denialReason) {
        // 도착 후 환불 시도는 중요하므로 알림
        if (denialReason.contains("도착 후")) {
            sendAlert(String.format(
                "[환불 거부] 도착 후 환불 시도\npaymentId: %d\n시도 시각: %s",
                paymentId, LocalDateTime.now()
            ));
            
            // 메트릭 기록
            meterRegistry.counter("refund.denied.after_arrival.alert").increment();
        }
    }
    
    /**
     * Unknown 상태 장시간 지속 알림
     */
    @Async
    public void alertLongUnknownStatus(int unknownCount) {
        if (unknownCount > 5) {
            sendUrgentAlert(String.format(
                "[주의] Unknown 상태 환불 %d건 존재\n" +
                "PG사 연동 상태 확인 필요",
                unknownCount
            ));
        }
    }
    
    /**
     * 중요한 실패인지 판단
     */
    private boolean isImportantFailure(String reason) {
        return reason.contains("중복") || 
               reason.contains("Unknown") || 
               reason.contains("PG") ||
               reason.contains("네트워크");
    }
    
    /**
     * 일반 알림 발송
     * 실제로는 Slack, Discord, 이메일 등으로 발송
     */
    private void sendAlert(String message) {
        // TODO: 실제 알림 채널 연동
        log.info("[ALERT] {}", message);
        
        // 실무에서는:
        // slackClient.sendMessage(alertChannel, message);
        // emailService.sendAlert(opsTeam, message);
    }
    
    /**
     * 긴급 알림 발송
     */
    private void sendUrgentAlert(String message) {
        // TODO: 실제 긴급 알림 채널 연동
        log.error("[URGENT ALERT] {}", message);
        
        // 실무에서는:
        // pagerDuty.triggerIncident(message);
        // slackClient.sendMessage(urgentChannel, "@channel " + message);
    }
    
    /**
     * 시간별 카운터 초기화 (스케줄러로 매시간 실행)
     */
    public void clearOldCounters() {
        String currentHour = LocalDateTime.now().toString().substring(0, 13);
        hourlyFailureCount.entrySet().removeIf(entry -> !entry.getKey().equals(currentHour));
    }
}