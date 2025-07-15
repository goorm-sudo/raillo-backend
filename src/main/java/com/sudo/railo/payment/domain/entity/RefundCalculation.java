package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.payment.domain.constant.PaymentPrecision;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환불 계산 엔티티
 * 시간대별 환불 수수료 계산 결과를 저장
 */
@Entity
@Table(name = "refund_calculations", indexes = {
    @Index(name = "idx_reservation_id", columnList = "reservation_id"),
    @Index(name = "idx_refund_request_time", columnList = "refund_request_time"),
    @Index(name = "idx_payment_id", columnList = "payment_id")
})
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
@Slf4j
public class RefundCalculation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_calculation_id")
    private Long id;
    
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;
    
    private Long memberId;
    
    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey; // 멱등성 보장을 위한 키
    
    @Column(name = "original_amount", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE, nullable = false)
    private BigDecimal originalAmount; // 원래 결제 금액
    
    @Column(name = "mileage_used", precision = PaymentPrecision.MILEAGE_PRECISION, scale = PaymentPrecision.MILEAGE_SCALE)
    @Builder.Default
    private BigDecimal mileageUsed = BigDecimal.ZERO; // 사용한 마일리지
    
    @Column(name = "refund_fee_rate", precision = PaymentPrecision.RATE_PRECISION, scale = PaymentPrecision.RATE_SCALE, nullable = false)
    private BigDecimal refundFeeRate; // 환불 수수료율 (0.300 = 30%)
    
    @Column(name = "refund_fee", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE, nullable = false)
    private BigDecimal refundFee; // 환불 수수료
    
    @Column(name = "refund_amount", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE, nullable = false)
    private BigDecimal refundAmount; // 실제 환불 금액
    
    @Column(name = "mileage_refund_amount", precision = PaymentPrecision.MILEAGE_PRECISION, scale = PaymentPrecision.MILEAGE_SCALE)
    @Builder.Default
    private BigDecimal mileageRefundAmount = BigDecimal.ZERO; // 마일리지 환불 금액
    
    @Column(name = "train_departure_time", nullable = false)
    private LocalDateTime trainDepartureTime; // 열차 출발 시간
    
    @Column(name = "train_arrival_time", nullable = false)
    private LocalDateTime trainArrivalTime; // 열차 도착 시간
    
    @Column(name = "refund_request_time", nullable = false)
    private LocalDateTime refundRequestTime; // 환불 요청 시간
    
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false)
    private RefundType refundType; // 환불 유형
    
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false)
    @Builder.Default
    private RefundStatus refundStatus = RefundStatus.PENDING; // 환불 처리 상태
    
    @Column(name = "refund_reason")
    private String refundReason; // 환불 사유
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt; // 환불 처리 완료 시간
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 환불 처리 완료 표시
     */
    public void markAsProcessed() {
        this.refundStatus = RefundStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 환불 처리 실패 표시
     */
    public void markAsFailed(String reason) {
        this.refundStatus = RefundStatus.FAILED;
        this.refundReason = reason;
    }
    
    /**
     * 환불 상태 업데이트
     */
    public void updateRefundStatus(RefundStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("환불 상태는 null일 수 없습니다");
        }
        this.refundStatus = status;
    }
    
    /**
     * 환불 사유 업데이트
     */
    public void updateRefundReason(String reason) {
        this.refundReason = reason;
    }
    
    /**
     * 환불 가능 여부 확인 (시간 기준)
     */
    public boolean isRefundableByTime() {
        if (trainArrivalTime == null) {
            log.warn("trainArrivalTime이 null입니다. refundCalculationId: {}", this.id);
            // trainArrivalTime이 없으면 기본적으로 환불 가능으로 처리 (임시)
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        // 열차 도착 시간 이후는 환불 불가
        boolean refundable = now.isBefore(trainArrivalTime);
        log.info("환불 가능 여부 확인 - 현재시간: {}, 도착시간: {}, 환불가능: {}", now, trainArrivalTime, refundable);
        return refundable;
    }
    
    /**
     * 환불 수수료율 계산
     * 철도 환불 정책에 따른 수수료율 계산
     * 
     * @deprecated RefundPolicyService를 사용하세요. 
     *             이 메서드는 하드코딩된 정책을 포함하고 있으며,
     *             운영사별 다른 정책을 적용할 수 없습니다.
     * @see com.sudo.railo.payment.domain.service.refund.RefundPolicyService
     * @param departureTime 열차 출발 시간
     * @param arrivalTime 열차 도착 시간
     * @param requestTime 환불 요청 시간
     * @return 환불 수수료율 (0.0 ~ 1.0)
     */
    @Deprecated
    public static BigDecimal calculateRefundFeeRate(LocalDateTime departureTime, LocalDateTime arrivalTime, LocalDateTime requestTime) {
        // 1. 도착 후 체크를 가장 먼저! (중요!)
        if (requestTime.isAfter(arrivalTime)) {
            return new BigDecimal("1.0"); // 100% 위약금 (환불 불가)
        }
        
        // 2. 출발 전 환불 - 위약금 없음
        if (requestTime.isBefore(departureTime)) {
            return BigDecimal.ZERO; // 0% 위약금
        }
        
        // 3. 출발 후 ~ 도착 전 환불
        long minutesAfterDeparture = java.time.Duration.between(departureTime, requestTime).toMinutes();
        
        if (minutesAfterDeparture <= 20) {
            return new BigDecimal("0.3"); // 30% 위약금
        } else if (minutesAfterDeparture <= 60) {
            return new BigDecimal("0.4"); // 40% 위약금
        } else {
            return new BigDecimal("0.7"); // 70% 위약금
        }
    }
    
    /**
     * 실패 사유 조회 (하위 호환성)
     * getRefundReason()의 별칭
     */
    public String getFailureReason() {
        return refundReason;
    }
} 