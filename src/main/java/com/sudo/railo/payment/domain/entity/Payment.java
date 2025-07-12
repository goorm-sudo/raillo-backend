package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.member.domain.Member;
import com.sudo.railo.payment.domain.constant.PaymentPrecision;
import com.sudo.railo.payment.domain.util.PaymentStatusMapper;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.train.domain.type.TrainOperator;
// import com.sudo.railo.payment.application.event.PaymentStateChangedEvent; // AbstractAggregateRoot 제거로 인해 사용하지 않음
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
// import org.springframework.data.domain.AbstractAggregateRoot; // 통합테스트를 위해 제거
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 결제 엔티티
 * 
 * DDD Aggregate Root로서 결제와 관련된 모든 정보를 관리합니다.
 * 회원/비회원 통합 처리, 결제 상태 관리, 환불 처리 등의 비즈니스 로직을 포함합니다.
 */
@Entity
@Table(name = "Payments", indexes = {
    @Index(name = "idx_payment_external_order_id", columnList = "external_order_id"),
    @Index(name = "idx_payment_reservation_id", columnList = "reservation_id"),
    @Index(name = "idx_payment_member_id", columnList = "member_id"),
    @Index(name = "idx_payment_created_at", columnList = "created_at")
})
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;
    
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;
    
    @Column(name = "external_order_id", nullable = false)
    private String externalOrderId;
    
    // 환불을 위한 열차 정보 (예약 삭제 시에도 환불 가능하도록)
    @Column(name = "train_schedule_id")
    private Long trainScheduleId;
    
    @Column(name = "train_departure_time")
    private LocalDateTime trainDepartureTime;
    
    @Column(name = "train_arrival_time")
    private LocalDateTime trainArrivalTime;
    
    @Column(name = "train_operator", length = 50)
    @Enumerated(EnumType.STRING)
    private TrainOperator trainOperator;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
    
    // 비회원 정보
    @Column(name = "non_member_name")
    private String nonMemberName;
    
    @Column(name = "non_member_phone")
    private String nonMemberPhone;
    
    @Column(name = "non_member_password")
    private String nonMemberPassword;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;
    
    @Column(name = "pg_provider")
    private String pgProvider;
    
    @Column(name = "amount_original_total", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE, nullable = false)
    private BigDecimal amountOriginalTotal;
    
    @Builder.Default
    @Column(name = "total_discount_amount_applied", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE)
    private BigDecimal totalDiscountAmountApplied = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "mileage_points_used", precision = PaymentPrecision.MILEAGE_PRECISION, scale = PaymentPrecision.MILEAGE_SCALE)
    private BigDecimal mileagePointsUsed = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "mileage_amount_deducted", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE)
    private BigDecimal mileageAmountDeducted = BigDecimal.ZERO;
    
    @Column(name = "amount_paid", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE, nullable = false)
    private BigDecimal amountPaid;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentExecutionStatus paymentStatus;
    
    private String pgTransactionId;
    
    @Column(name = "pg_approval_no")
    private String pgApprovalNo;
    
    // 현금영수증 정보 (Value Object)
    @Embedded
    private CashReceipt cashReceipt;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
    
    @Builder.Default
    @Column(name = "refund_amount", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE)
    private BigDecimal refundAmount = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "refund_fee", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE)
    private BigDecimal refundFee = BigDecimal.ZERO;
    
    @Column(name = "refund_reason")
    private String refundReason;
    
    private String pgRefundTransactionId;
    
    @Column(name = "pg_refund_approval_no")
    private String pgRefundApprovalNo;
    
    @Builder.Default
    @Column(name = "mileage_to_earn", precision = PaymentPrecision.MILEAGE_PRECISION, scale = PaymentPrecision.MILEAGE_SCALE)
    private BigDecimal mileageToEarn = BigDecimal.ZERO;
    
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // Soft Delete 필드
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "deletion_reason")
    private String deletionReason;
    
    // =========================== Public Methods ===========================
    
    /**
     * 결제 상태 업데이트 (상태 전이 검증 포함)
     * 
     * @param newStatus 변경할 새로운 상태
     * @throws PaymentValidationException 유효하지 않은 상태 전이 시
     */
    public void updateStatus(PaymentExecutionStatus newStatus) {
        updateStatus(newStatus, null, "SYSTEM");
    }
    
    /**
     * 결제 상태 업데이트 (상세 정보 포함)
     * 
     * @param newStatus 변경할 새로운 상태
     * @param reason 상태 변경 사유
     * @param triggeredBy 상태 변경 주체
     * @throws PaymentValidationException 유효하지 않은 상태 전이 시
     */
    public void updateStatus(PaymentExecutionStatus newStatus, String reason, String triggeredBy) {
        // 현재 상태 저장 (이벤트용)
        PaymentExecutionStatus previousStatus = this.paymentStatus;
        
        // 상태 전이 검증
        validateStatusTransition(this.paymentStatus, newStatus);
        
        // 상태 변경
        this.paymentStatus = newStatus;
        
        // 상태별 추가 처리
        if (newStatus == PaymentExecutionStatus.SUCCESS) {
            if (this.paidAt == null) { // 이미 설정되어 있지 않은 경우에만
                this.paidAt = LocalDateTime.now();
            }
        } else if (newStatus == PaymentExecutionStatus.CANCELLED) {
            this.cancelledAt = LocalDateTime.now();
        } else if (newStatus == PaymentExecutionStatus.FAILED) {
            // 실패 시 처리
        }
        
        // 이벤트 발행은 Application Service 계층에서 PaymentEventPublisher를 통해 처리
        // AbstractAggregateRoot 제거로 인해 registerEvent 호출 제거
    }
    
    /**
     * PG 정보 업데이트
     * 
     * @param pgTransactionId PG 거래 ID
     * @param pgApprovalNo PG 승인 번호
     */
    public void updatePgInfo(String pgTransactionId, String pgApprovalNo) {
        this.pgTransactionId = pgTransactionId;
        this.pgApprovalNo = pgApprovalNo;
    }
    
    /**
     * 환불 가능 여부 확인
     * 
     * @return 환불 가능하면 true
     */
    public boolean isRefundable() {
        return this.paymentStatus == PaymentExecutionStatus.SUCCESS && 
               this.paidAt != null && 
               this.paidAt.isAfter(LocalDateTime.now().minusDays(30)); // 30일 이내만 환불 가능
    }
    
    /**
     * 취소 가능 여부 확인
     * 
     * @return 취소 가능하면 true
     */
    public boolean isCancellable() {
        return PaymentStatusMapper.isInProgress(this.paymentStatus);
    }
    
    /**
     * 결제 완료 여부 확인
     * 
     * @return 결제 완료 상태면 true
     */
    public boolean isCompleted() {
        return PaymentStatusMapper.isCompleted(this.paymentStatus);
    }
    
    /**
     * 회원 결제 여부 확인
     * 
     * @return 회원 결제면 true
     */
    public boolean isForMember() {
        return this.member != null;
    }
    
    /**
     * 회원 ID 조회 (하위 호환성)
     * 
     * @return 회원 ID, 비회원인 경우 null
     */
    public Long getMemberId() {
        return this.member != null ? this.member.getId() : null;
    }
    
    /**
     * 현금영수증 URL 조회 (하위 호환성)
     * 
     * @return 현금영수증 URL, 없는 경우 null
     */
    public String getReceiptUrl() {
        return this.cashReceipt != null ? this.cashReceipt.getReceiptUrl() : null;
    }
    
    /**
     * 실제 결제 금액 계산 (마일리지 차감 후)
     * 
     * @return 계산된 실제 결제 금액
     */
    public BigDecimal calculateNetAmount() {
        BigDecimal originalAmount = this.amountOriginalTotal != null ? this.amountOriginalTotal : BigDecimal.ZERO;
        BigDecimal discountAmount = this.totalDiscountAmountApplied != null ? this.totalDiscountAmountApplied : BigDecimal.ZERO;
        BigDecimal mileageDeducted = this.mileageAmountDeducted != null ? this.mileageAmountDeducted : BigDecimal.ZERO;
        
        return originalAmount.subtract(discountAmount).subtract(mileageDeducted);
    }
    
    // =========================== Private Methods ===========================
    
    /**
     * 상태 전이 규칙 검증
     * 
     * @param from 현재 상태
     * @param to 변경할 상태
     * @throws PaymentValidationException 유효하지 않은 상태 전이 시
     */
    private void validateStatusTransition(PaymentExecutionStatus from, PaymentExecutionStatus to) {
        switch (from) {
            case PENDING:
                if (!Arrays.asList(PaymentExecutionStatus.PROCESSING, PaymentExecutionStatus.CANCELLED, 
                                 PaymentExecutionStatus.FAILED).contains(to)) {
                    throw new PaymentValidationException(
                        String.format("유효하지 않은 상태 전이: %s → %s", from, to));
                }
                break;
            case PROCESSING:
                if (!Arrays.asList(PaymentExecutionStatus.SUCCESS, PaymentExecutionStatus.FAILED, 
                                 PaymentExecutionStatus.CANCELLED).contains(to)) {
                    throw new PaymentValidationException(
                        String.format("유효하지 않은 상태 전이: %s → %s", from, to));
                }
                break;
            case SUCCESS:
                if (!Arrays.asList(PaymentExecutionStatus.REFUNDED, PaymentExecutionStatus.CANCELLED)
                        .contains(to)) {
                    throw new PaymentValidationException(
                        String.format("유효하지 않은 상태 전이: %s → %s", from, to));
                }
                break;
            case FAILED:
            case CANCELLED:
            case REFUNDED:
                throw new PaymentValidationException(
                    String.format("종료 상태에서는 변경할 수 없습니다: %s", from));
            default:
                throw new PaymentValidationException(
                    String.format("알 수 없는 상태: %s", from));
        }
    }
    
    /**
     * 결제 취소 처리
     */
    public void cancel(String reason) {
        if (!isCancellable()) {
            throw new PaymentValidationException(
                String.format("취소할 수 없는 상태입니다: %s", this.paymentStatus));
        }
        
        this.updateStatus(PaymentExecutionStatus.CANCELLED, reason, "USER");
        this.refundReason = reason;
    }
    
    /**
     * 환불 처리
     */
    public void processRefund(RefundRequest refundRequest) {
        if (!isRefundable()) {
            throw new PaymentValidationException(
                String.format("환불할 수 없는 상태입니다: %s", this.paymentStatus));
        }
        
        // 환불 금액 검증
        if (refundRequest.getRefundAmount().compareTo(this.amountPaid) > 0) {
            throw new PaymentValidationException("환불 금액이 결제 금액보다 큽니다");
        }
        
        this.updateStatus(PaymentExecutionStatus.REFUNDED, refundRequest.getReason(), "ADMIN");
        this.refundedAt = LocalDateTime.now();
        this.refundAmount = refundRequest.getRefundAmount();
        this.refundFee = refundRequest.getRefundFee();
        this.refundReason = refundRequest.getReason();
        this.pgRefundTransactionId = refundRequest.getPgTransactionId();
        this.pgRefundApprovalNo = refundRequest.getPgApprovalNo();
    }
    
    /**
     * Soft Delete 처리
     * 
     * @param reason 삭제 사유
     */
    public void softDelete(String reason) {
        this.deletedAt = LocalDateTime.now();
        this.deletionReason = reason;
    }
    
    /**
     * Soft Delete 여부 확인
     * 
     * @return 삭제되었으면 true
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
    
    // =========================== Inner Classes ===========================
    
    /**
     * 환불 요청 정보 DTO
     */
    @Builder
    @Getter
    public static class RefundRequest {
        private final BigDecimal refundAmount;
        private final BigDecimal refundFee;
        private final String reason;
        private final String pgTransactionId;
        private final String pgApprovalNo;
    }
} 