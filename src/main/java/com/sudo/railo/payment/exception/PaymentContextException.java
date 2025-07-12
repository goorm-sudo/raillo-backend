package com.sudo.railo.payment.exception;

/**
 * 결제 컨텍스트 관련 예외
 * 
 * PaymentContext 생성 및 검증 과정에서 발생하는 예외
 */
public class PaymentContextException extends PaymentException {
    
    public PaymentContextException(String message) {
        super(message);
    }
    
    public PaymentContextException(String message, Throwable cause) {
        super(message, cause);
    }
}