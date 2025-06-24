package com.sudo.railo.payment.exception;

public class PaymentValidationException extends PaymentException {
    
    public PaymentValidationException(String message) {
        super(message, "PAYMENT_VALIDATION_ERROR");
    }
    
    public PaymentValidationException(String message, Throwable cause) {
        super(message, "PAYMENT_VALIDATION_ERROR", cause);
    }
} 