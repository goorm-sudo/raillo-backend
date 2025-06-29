package com.sudo.railo.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PaymentCalculations")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentCalculation {
    
    @Id
    @Column(name = "calculation_id", length = 36)
    private String calculationId;
    
    @Column(name = "external_order_id", nullable = false)
    private String externalOrderId;
    
    @Column(name = "user_id_external", nullable = false)
    private String userIdExternal;
    
    @Column(name = "original_amount", precision = 12, scale = 0)
    private BigDecimal originalAmount;
    
    @Column(name = "final_amount", precision = 12, scale = 0)
    private BigDecimal finalAmount;
    
    @Builder.Default
    @Column(name = "mileage_to_use", precision = 12, scale = 0)
    private BigDecimal mileageToUse = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "available_mileage", precision = 12, scale = 0)
    private BigDecimal availableMileage = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "mileage_discount", precision = 12, scale = 0)
    private BigDecimal mileageDiscount = BigDecimal.ZERO;
    
    @Column(name = "promotion_snapshot", columnDefinition = "JSON")
    private String promotionSnapshot;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CalculationStatus status = CalculationStatus.CALCULATED;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
} 