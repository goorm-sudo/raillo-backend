package com.sudo.railo.payment.exception;

import java.math.BigDecimal;

/**
 * 결제 금액 한도 초과 예외
 * 최대/최소 결제 금액을 벗어날 때 발생
 */
public class PaymentAmountLimitExceededException extends PaymentValidationException {
    
    private final BigDecimal requestedAmount;
    private final BigDecimal limitAmount;
    private final LimitType limitType;
    
    public enum LimitType {
        MAX_AMOUNT("최대 결제 금액"),
        MIN_AMOUNT("최소 결제 금액"),
        MAX_NON_MEMBER_AMOUNT("비회원 최대 결제 금액");
        
        private final String description;
        
        LimitType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public PaymentAmountLimitExceededException(BigDecimal requestedAmount, BigDecimal limitAmount, LimitType limitType) {
        super(String.format("%s 초과. 요청 금액: %s, 제한 금액: %s", 
                limitType.getDescription(), requestedAmount, limitAmount));
        this.requestedAmount = requestedAmount;
        this.limitAmount = limitAmount;
        this.limitType = limitType;
    }
    
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
    
    public BigDecimal getLimitAmount() {
        return limitAmount;
    }
    
    public LimitType getLimitType() {
        return limitType;
    }
}
