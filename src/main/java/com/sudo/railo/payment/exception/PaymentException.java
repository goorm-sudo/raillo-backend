package com.sudo.railo.payment.exception;

public class PaymentException extends RuntimeException {
    
    private final String errorCode;
    
    public PaymentException(String message) {
        super(message);
        this.errorCode = "PAYMENT_ERROR";
    }
    
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PAYMENT_ERROR";
    }
    
    public PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public PaymentException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
} 