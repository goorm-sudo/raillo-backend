package com.sudo.railo.payment.exception;

/**
 * 결제 정보를 찾을 수 없을 때 발생하는 예외
 */
public class PaymentNotFoundException extends PaymentException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}