package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payments")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;
    
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;
    
    @Column(name = "external_order_id", nullable = false)
    private String externalOrderId;
    
    @Column(name = "member_id")
    private Long memberId;
    
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
    
    @Column(name = "amount_original_total", precision = 12, scale = 0)
    private BigDecimal amountOriginalTotal;
    
    @Builder.Default
    @Column(name = "total_discount_amount_applied", precision = 12, scale = 0)
    private BigDecimal totalDiscountAmountApplied = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "mileage_points_used", precision = 12, scale = 0)
    private BigDecimal mileagePointsUsed = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "mileage_amount_deducted", precision = 12, scale = 0)
    private BigDecimal mileageAmountDeducted = BigDecimal.ZERO;
    
    @Column(name = "amount_paid", precision = 12, scale = 0)
    private BigDecimal amountPaid;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentExecutionStatus paymentStatus;
    
    @Column(name = "pg_transaction_id")
    private String pgTransactionId;
    
    @Column(name = "pg_approval_no")
    private String pgApprovalNo;
    
    // 현금영수증 관련 필드
    @Builder.Default
    @Column(name = "receipt_requested")
    private Boolean receiptRequested = false; // 현금영수증 신청 여부
    
    @Column(name = "receipt_type")
    private String receiptType; // 현금영수증 타입: "personal" 또는 "business"
    
    @Column(name = "receipt_phone_number")
    private String receiptPhoneNumber; // 개인 소득공제용 휴대폰 번호
    
    @Column(name = "receipt_business_number")
    private String receiptBusinessNumber; // 사업자 증빙용 사업자등록번호
    
    @Column(name = "receipt_url")
    private String receiptUrl;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
    
    @Builder.Default
    @Column(name = "refund_amount", precision = 12, scale = 0)
    private BigDecimal refundAmount = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "refund_fee", precision = 12, scale = 0)
    private BigDecimal refundFee = BigDecimal.ZERO;
    
    @Column(name = "refund_reason")
    private String refundReason;
    
    @Column(name = "pg_refund_transaction_id")
    private String pgRefundTransactionId;
    
    @Column(name = "pg_refund_approval_no")
    private String pgRefundApprovalNo;
    
    @Builder.Default
    @Column(name = "mileage_to_earn", precision = 12, scale = 0)
    private BigDecimal mileageToEarn = BigDecimal.ZERO;
    
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 결제 상태 업데이트
     */
    public void updateStatus(PaymentExecutionStatus newStatus) {
        this.paymentStatus = newStatus;
        if (newStatus == PaymentExecutionStatus.SUCCESS) {
            this.paidAt = LocalDateTime.now();
        }
    }
    
    /**
     * 결제 취소 처리
     */
    public void cancel(String reason) {
        this.paymentStatus = PaymentExecutionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.refundReason = reason;
    }
    
    /**
     * 환불 처리 (전체 환불)
     */
    public void refund(BigDecimal refundAmount, BigDecimal refundFee, String reason, 
                      String pgRefundTransactionId, String pgRefundApprovalNo) {
        this.paymentStatus = PaymentExecutionStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
        this.refundAmount = refundAmount;
        this.refundFee = refundFee;
        this.refundReason = reason;
        this.pgRefundTransactionId = pgRefundTransactionId;
        this.pgRefundApprovalNo = pgRefundApprovalNo;
    }
    
    /**
     * 환불 가능 여부 확인
     */
    public boolean isRefundable() {
        return this.paymentStatus == PaymentExecutionStatus.SUCCESS;
    }
    
    /**
     * 취소 가능 여부 확인
     */
    public boolean isCancellable() {
        return this.paymentStatus == PaymentExecutionStatus.PENDING 
            || this.paymentStatus == PaymentExecutionStatus.PROCESSING;
    }
} 