package com.sudo.railo.payment.domain.entity;

public enum CalculationStatus {
    CALCULATED,     // 계산 완료
    EXPIRED,        // 만료됨
    CONSUMED,       // 사용됨 (기존 호환성 유지)
    USED           // 사용됨 (신규)
} 