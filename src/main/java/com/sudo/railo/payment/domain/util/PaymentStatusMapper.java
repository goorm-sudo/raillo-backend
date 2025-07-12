package com.sudo.railo.payment.domain.util;

import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.booking.domain.PaymentStatus;

/**
 * 결제 상태 매핑 유틸리티
 * 
 * Payment 도메인의 PaymentExecutionStatus와 Booking 도메인의 PaymentStatus 간의 매핑을 담당
 * 
 * 매핑 규칙:
 * - PENDING/PROCESSING → RESERVED (예약 상태)
 * - SUCCESS → PAID (결제 완료)
 * - CANCELLED → CANCELLED (취소)
 * - REFUNDED → REFUNDED (환불)
 * - FAILED → CANCELLED (실패는 취소로 처리)
 */
public class PaymentStatusMapper {
    
    private PaymentStatusMapper() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }
    
    /**
     * PaymentExecutionStatus를 Booking PaymentStatus로 변환
     * 
     * @param executionStatus 결제 실행 상태
     * @return 예약 시스템 결제 상태
     * @throws IllegalArgumentException 지원하지 않는 상태인 경우
     */
    public static PaymentStatus toBookingPaymentStatus(PaymentExecutionStatus executionStatus) {
        if (executionStatus == null) {
            throw new IllegalArgumentException("PaymentExecutionStatus는 null일 수 없습니다");
        }
        
        switch (executionStatus) {
            case PENDING:
            case PROCESSING:
                return PaymentStatus.RESERVED;
            case SUCCESS:
                return PaymentStatus.PAID;
            case CANCELLED:
                return PaymentStatus.CANCELLED;
            case REFUNDED:
                return PaymentStatus.REFUNDED;
            case FAILED:
                return PaymentStatus.CANCELLED; // 실패는 취소로 처리
            default:
                throw new IllegalArgumentException("지원하지 않는 PaymentExecutionStatus: " + executionStatus);
        }
    }
    
    /**
     * Booking PaymentStatus를 PaymentExecutionStatus로 변환
     * 
     * @param bookingStatus 예약 시스템 결제 상태
     * @return 결제 실행 상태
     * @throws IllegalArgumentException 지원하지 않는 상태인 경우
     */
    public static PaymentExecutionStatus toPaymentExecutionStatus(PaymentStatus bookingStatus) {
        if (bookingStatus == null) {
            throw new IllegalArgumentException("PaymentStatus는 null일 수 없습니다");
        }
        
        switch (bookingStatus) {
            case RESERVED:
                return PaymentExecutionStatus.PENDING;
            case PAID:
                return PaymentExecutionStatus.SUCCESS;
            case CANCELLED:
                return PaymentExecutionStatus.CANCELLED;
            case REFUNDED:
                return PaymentExecutionStatus.REFUNDED;
            default:
                throw new IllegalArgumentException("지원하지 않는 PaymentStatus: " + bookingStatus);
        }
    }
    
    /**
     * 결제 실행 상태가 완료된 상태인지 확인
     * 
     * @param executionStatus 결제 실행 상태
     * @return 완료된 상태(SUCCESS, CANCELLED, REFUNDED)인 경우 true
     */
    public static boolean isCompleted(PaymentExecutionStatus executionStatus) {
        return executionStatus == PaymentExecutionStatus.SUCCESS
            || executionStatus == PaymentExecutionStatus.CANCELLED
            || executionStatus == PaymentExecutionStatus.REFUNDED
            || executionStatus == PaymentExecutionStatus.FAILED;
    }
    
    /**
     * 결제 실행 상태가 진행 중인지 확인
     * 
     * @param executionStatus 결제 실행 상태
     * @return 진행 중인 상태(PENDING, PROCESSING)인 경우 true
     */
    public static boolean isInProgress(PaymentExecutionStatus executionStatus) {
        return executionStatus == PaymentExecutionStatus.PENDING
            || executionStatus == PaymentExecutionStatus.PROCESSING;
    }
} 