package com.sudo.railo.payment.exception;

import java.math.BigDecimal;

/**
 * 마일리지 부족 예외
 * 사용 가능한 마일리지보다 많은 마일리지를 사용하려 할 때 발생
 */
public class InsufficientMileageException extends PaymentValidationException {
    
    private final BigDecimal requestedAmount;
    private final BigDecimal availableAmount;
    
    public InsufficientMileageException(BigDecimal requestedAmount, BigDecimal availableAmount) {
        super(String.format("마일리지가 부족합니다. 요청: %s, 사용가능: %s", 
                requestedAmount, availableAmount));
        this.requestedAmount = requestedAmount;
        this.availableAmount = availableAmount;
    }
    
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
    
    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }
} 