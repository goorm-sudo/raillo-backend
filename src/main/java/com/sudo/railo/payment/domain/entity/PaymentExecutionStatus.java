package com.sudo.railo.payment.domain.entity;

/**
 * 결제 실행 상태
 * 
 * PG사와의 결제 처리 과정에서 발생하는 기술적 상태를 나타냄
 * booking 도메인의 PaymentStatus(비즈니스 관점)와 구분하여 사용
 * 
 * @see com.sudo.railo.booking.domain.PaymentStatus (비즈니스 상태)
 */
public enum PaymentExecutionStatus {
    PENDING,        // 결제 대기 (계산 완료, PG 요청 전)
    PROCESSING,     // 결제 처리 중 (PG사 처리 중)
    SUCCESS,        // 결제 성공 (PG사 승인 완료)
    FAILED,         // 결제 실패 (PG사 승인 거부)
    CANCELLED,      // 결제 취소 (결제 전 사용자 취소)
    REFUNDED        // 환불 완료 (결제 후 전체 환불)
} 