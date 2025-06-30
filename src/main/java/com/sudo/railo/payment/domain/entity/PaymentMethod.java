package com.sudo.railo.payment.domain.entity;

/**
 * 결제 수단 enum
 * PG사별 결제 방법 정의
 */
public enum PaymentMethod {
    
    // 카카오페이
    KAKAO_PAY("KAKAO_PAY", "카카오페이", "kakao"),
    
    // 네이버페이  
    NAVER_PAY("NAVER_PAY", "네이버페이", "naver"),
    
    // PAYCO
    PAYCO("PAYCO", "PAYCO", "payco"),
    
    // 신용카드 (직접 PG)
    CREDIT_CARD("CREDIT_CARD", "신용카드", "card"),
    
    // 내 통장 결제
    BANK_ACCOUNT("BANK_ACCOUNT", "내 통장 결제", "bank"),
    
    // 계좌이체
    BANK_TRANSFER("BANK_TRANSFER", "계좌이체", "trans"),
    
    // 마일리지 (내부 포인트)
    MILEAGE("MILEAGE", "마일리지", "mileage");
    
    private final String code;
    private final String displayName;
    private final String pgType;
    
    PaymentMethod(String code, String displayName, String pgType) {
        this.code = code;
        this.displayName = displayName;
        this.pgType = pgType;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getPgType() {
        return pgType;
    }
    
    /**
     * PG 타입으로 결제 수단 조회
     */
    public static PaymentMethod fromPgType(String pgType) {
        for (PaymentMethod method : values()) {
            if (method.pgType.equals(pgType)) {
                return method;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 PG 타입: " + pgType);
    }
    
    /**
     * 외부 PG사 연동이 필요한 결제 수단인지 확인
     */
    public boolean requiresExternalPg() {
        return this == KAKAO_PAY || this == NAVER_PAY || this == PAYCO || this == CREDIT_CARD || this == BANK_ACCOUNT || this == BANK_TRANSFER;
    }
    
    /**
     * 내부 처리 가능한 결제 수단인지 확인  
     */
    public boolean isInternalPayment() {
        return this == MILEAGE;
    }
} 