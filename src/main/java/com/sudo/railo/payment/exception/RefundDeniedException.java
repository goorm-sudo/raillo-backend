package com.sudo.railo.payment.exception;

/**
 * 환불이 거부될 때 발생하는 예외
 * 사용자에게 명확한 거부 사유를 전달하기 위한 전용 예외
 */
public class RefundDeniedException extends PaymentException {
    
    public RefundDeniedException(String message) {
        super(message);
    }
    
    public RefundDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}