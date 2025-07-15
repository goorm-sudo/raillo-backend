package com.sudo.railo.payment.exception;

/**
 * 결제 검증 실패 예외
 * 
 * 결제 과정에서 발생하는 검증 관련 예외를 나타냄
 * 예: 금액 불일치, 상태 오류, 세션 만료 등
 */
public class PaymentValidationException extends PaymentException {
    
    public PaymentValidationException(String message) {
        super(message);
    }
    
    public PaymentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
} 