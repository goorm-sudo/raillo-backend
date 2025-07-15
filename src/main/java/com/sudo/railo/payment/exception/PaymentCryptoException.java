package com.sudo.railo.payment.exception;

/**
 * 결제 정보 암호화/복호화 관련 예외
 */
public class PaymentCryptoException extends PaymentException {

    public PaymentCryptoException(String message) {
        super(message);
    }

    public PaymentCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}