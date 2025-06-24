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
    private String reservationId;
    
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
    
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;
    
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
    private PaymentStatus paymentStatus;
    
    @Column(name = "pg_transaction_id")
    private String pgTransactionId;
    
    @Column(name = "pg_approval_no")
    private String pgApprovalNo;
    
    @Column(name = "receipt_url")
    private String receiptUrl;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
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
    public void updateStatus(PaymentStatus newStatus) {
        this.paymentStatus = newStatus;
        if (newStatus == PaymentStatus.SUCCESS) {
            this.paidAt = LocalDateTime.now();
        }
    }
} 