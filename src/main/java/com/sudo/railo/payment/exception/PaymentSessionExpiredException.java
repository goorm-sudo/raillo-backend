package com.sudo.railo.payment.exception;

import java.time.LocalDateTime;

/**
 * 결제 세션 만료 예외
 * 계산 세션이 만료되어 결제할 수 없을 때 발생
 */
public class PaymentSessionExpiredException extends PaymentValidationException {
    
    private final String sessionId;
    private final LocalDateTime expiredAt;
    
    public PaymentSessionExpiredException(String sessionId, LocalDateTime expiredAt) {
        super(String.format("결제 세션이 만료되었습니다. 세션ID: %s, 만료시간: %s", 
                sessionId, expiredAt));
        this.sessionId = sessionId;
        this.expiredAt = expiredAt;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }
}
