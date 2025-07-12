package com.sudo.railo.payment.domain.entity;

/**
 * 환불 처리 상태
 */
public enum RefundStatus {
    PENDING("환불 대기"),
    PROCESSING("환불 처리 중"),
    COMPLETED("환불 완료"),
    FAILED("환불 실패"),
    CANCELLED("환불 취소"),
    UNKNOWN("상태 불명"),      // 네트워크 오류 등으로 결과를 알 수 없는 상태
    ATTEMPTED("시도됨");       // 환불 시도 기록용
    
    private final String description;
    
    RefundStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 