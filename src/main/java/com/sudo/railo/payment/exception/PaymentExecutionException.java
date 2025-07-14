package com.sudo.railo.payment.exception;

/**
 * 결제 실행 관련 예외
 * 
 * 결제 실행 과정에서 발생하는 예외 (PG 연동, 마일리지 차감 등)
 */
public class PaymentExecutionException extends PaymentException {
    
    public PaymentExecutionException(String message) {
        super(message);
    }
    
    public PaymentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}