package com.sudo.railo.payment.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 환불 완료 이벤트
 * Booking 도메인에서 예약 상태를 업데이트하기 위해 발행
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundedEvent {
    
    private Long paymentId;
    private Long reservationId;
    private String externalOrderId;
    private Long memberId;
    private BigDecimal refundAmount;
    private BigDecimal refundFee;
    private String refundReason;
    private String pgRefundTransactionId;
    private LocalDateTime refundedAt;
    
    // 마일리지 복구 정보
    private BigDecimal mileageToRestore;      // 복구할 마일리지
    private BigDecimal mileageEarnedToCancel; // 취소할 적립 예정 마일리지
} 