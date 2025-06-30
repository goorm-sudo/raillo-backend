package com.sudo.railo.payment.domain.entity;

/**
 * 결제 실행 상태
 * booking 도메인의 PaymentStatus와 구분하여 결제 처리 과정의 상태를 나타냄
 */
public enum PaymentExecutionStatus {
    PENDING,        // 결제 대기
    PROCESSING,     // 결제 처리 중
    SUCCESS,        // 결제 성공
    FAILED,         // 결제 실패
    CANCELLED,      // 결제 취소 (결제 전 취소)
    REFUNDED        // 환불 완료 (결제 후 전체 환불)
} 