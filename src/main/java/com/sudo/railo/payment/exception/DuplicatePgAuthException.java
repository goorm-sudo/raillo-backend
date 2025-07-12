package com.sudo.railo.payment.exception;

/**
 * PG 승인번호 중복 사용 예외
 */
public class DuplicatePgAuthException extends RuntimeException {
    
    public DuplicatePgAuthException(String message) {
        super(message);
    }
    
    public DuplicatePgAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}