package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.payment.domain.constant.PaymentPrecision;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PaymentCalculations")
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentCalculation {
    
    @Id
    @Column(name = "calculation_id", length = 36)
    private String id;
    
    @Column(name = "reservation_id")
    private String reservationId;
    
    @Column(name = "external_order_id", nullable = false)
    private String externalOrderId;
    
    @Column(name = "user_id_external", nullable = false)
    private String userIdExternal;
    
    @Column(name = "original_amount", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE, nullable = false)
    private BigDecimal originalAmount;
    
    @Column(name = "final_amount", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE, nullable = false)
    private BigDecimal finalAmount;
    
    @Builder.Default
    @Column(name = "mileage_to_use", precision = PaymentPrecision.MILEAGE_PRECISION, scale = PaymentPrecision.MILEAGE_SCALE)
    private BigDecimal mileageToUse = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "available_mileage", precision = PaymentPrecision.MILEAGE_PRECISION, scale = PaymentPrecision.MILEAGE_SCALE)
    private BigDecimal availableMileage = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "mileage_discount", precision = PaymentPrecision.AMOUNT_PRECISION, scale = PaymentPrecision.AMOUNT_SCALE)
    private BigDecimal mileageDiscount = BigDecimal.ZERO;
    
    @Column(name = "promotion_snapshot", columnDefinition = "JSON")
    private String promotionSnapshot;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CalculationStatus status = CalculationStatus.CALCULATED;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    // 열차 정보 (예약 삭제 시에도 결제 가능하도록)
    @Column(name = "train_schedule_id")
    private Long trainScheduleId;
    
    @Column(name = "train_departure_time")
    private LocalDateTime trainDepartureTime;
    
    @Column(name = "train_arrival_time")
    private LocalDateTime trainArrivalTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "train_operator", length = 50)
    private com.sudo.railo.train.domain.type.TrainOperator trainOperator;
    
    @Column(name = "route_info", length = 100)
    private String routeInfo; // 예: "서울-부산"
    
    // 보안 강화 필드
    @Column(name = "seat_number", length = 10)
    private String seatNumber;
    
    @Column(name = "pg_order_id", unique = true)
    private String pgOrderId; // PG사에 전달할 주문번호
    
    @Column(name = "created_by_ip", length = 45)
    private String createdByIp;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt; // 결제 완료 시점
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // =========================== Business Methods ===========================
    
    /**
     * 계산 만료 처리
     */
    public void markAsExpired() {
        this.status = CalculationStatus.EXPIRED;
    }
    
    /**
     * 최종 금액 업데이트
     * 
     * @param newFinalAmount 새로운 최종 금액
     */
    public void updateFinalAmount(BigDecimal newFinalAmount) {
        if (newFinalAmount == null || newFinalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("최종 금액은 0 이상이어야 합니다");
        }
        this.finalAmount = newFinalAmount;
    }
    
    /**
     * 마일리지 할인 적용
     * 
     * @param mileageToUse 사용할 마일리지 포인트
     * @param mileageDiscount 마일리지 할인 금액
     */
    public void applyMileageDiscount(BigDecimal mileageToUse, BigDecimal mileageDiscount) {
        if (mileageToUse == null || mileageToUse.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("사용 마일리지는 0 이상이어야 합니다");
        }
        if (mileageDiscount == null || mileageDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("마일리지 할인 금액은 0 이상이어야 합니다");
        }
        
        this.mileageToUse = mileageToUse;
        this.mileageDiscount = mileageDiscount;
        
        // 최종 금액 재계산
        this.finalAmount = this.originalAmount.subtract(this.mileageDiscount);
        if (this.finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.finalAmount = BigDecimal.ZERO;
        }
    }
    
    /**
     * 계산이 유효한지 확인
     * 
     * @return 유효하면 true
     */
    public boolean isValid() {
        return this.status == CalculationStatus.CALCULATED && 
               this.expiresAt != null && 
               this.expiresAt.isAfter(LocalDateTime.now());
    }
    
    /**
     * 계산이 만료되었는지 확인
     * 
     * @return 만료되었으면 true
     */
    public boolean isExpired() {
        return this.status == CalculationStatus.EXPIRED || 
               (this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now()));
    }
    
    /**
     * 계산 세션을 사용됨으로 표시
     */
    public void markAsUsed() {
        this.status = CalculationStatus.USED;
        this.usedAt = LocalDateTime.now();
    }
    
    /**
     * 사용자 검증
     * 
     * @param userId 검증할 사용자 ID
     * @return 일치하면 true
     */
    public boolean isOwnedBy(String userId) {
        return this.userIdExternal != null && this.userIdExternal.equals(userId);
    }
} 