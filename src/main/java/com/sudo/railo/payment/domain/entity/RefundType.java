package com.sudo.railo.payment.domain.entity;

/**
 * 환불 유형
 */
public enum RefundType {
    CHANGE("변경"),
    CANCEL("취소"),
    FULL("전체 환불");
    
    private final String description;
    
    RefundType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 