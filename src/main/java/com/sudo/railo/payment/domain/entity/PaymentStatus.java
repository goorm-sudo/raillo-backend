package com.sudo.railo.payment.domain.entity;

public enum PaymentStatus {
    PENDING,        // 결제 대기
    PROCESSING,     // 결제 처리 중
    SUCCESS,        // 결제 성공
    FAILED,         // 결제 실패
    CANCELLED       // 결제 취소
} 