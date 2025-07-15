package com.sudo.railo.payment.domain.entity;

/**
 * 회원 타입 구분 Enum
 */
public enum MemberType {
    MEMBER("회원"),
    NON_MEMBER("비회원");
    
    private final String description;
    
    MemberType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 