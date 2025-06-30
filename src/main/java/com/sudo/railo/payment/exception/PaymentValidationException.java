package com.sudo.railo.payment.exception;

/**
 * 결제 검증 예외
 */
public class PaymentValidationException extends RuntimeException {
    
    public PaymentValidationException(String message) {
        super(message);
    }
    
    public PaymentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
} 