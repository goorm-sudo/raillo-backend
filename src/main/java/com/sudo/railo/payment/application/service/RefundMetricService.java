package com.sudo.railo.payment.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 환불 메트릭 서비스
 * 
 * 단순하게 카운터만 관리합니다.
 * 실무에서 가장 많이 사용하는 패턴입니다.
 */
@Slf4j
@Service
public class RefundMetricService {
    
    private final Counter refundAttemptCounter;
    private final Counter refundSuccessCounter;
    private final Counter refundDeniedAfterArrivalCounter;
    private final Counter refundDeniedInvalidStatusCounter;
    
    public RefundMetricService(MeterRegistry registry) {
        this.refundAttemptCounter = Counter.builder("refund.attempt")
                .description("환불 시도 횟수")
                .register(registry);
                
        this.refundSuccessCounter = Counter.builder("refund.success")
                .description("환불 성공 횟수")
                .register(registry);
                
        this.refundDeniedAfterArrivalCounter = Counter.builder("refund.denied")
                .tag("reason", "after_arrival")
                .description("도착 후 환불 거부")
                .register(registry);
                
        this.refundDeniedInvalidStatusCounter = Counter.builder("refund.denied")
                .tag("reason", "invalid_status")
                .description("잘못된 상태로 환불 거부")
                .register(registry);
    }
    
    public void recordAttempt() {
        refundAttemptCounter.increment();
    }
    
    public void recordSuccess() {
        refundSuccessCounter.increment();
    }
    
    public void recordDeniedAfterArrival() {
        refundDeniedAfterArrivalCounter.increment();
    }
    
    public void recordDeniedInvalidStatus() {
        refundDeniedInvalidStatusCounter.increment();
    }
}