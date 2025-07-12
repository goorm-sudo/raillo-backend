package com.sudo.railo.payment.application.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentCalculationResponse {
    
    private String calculationId;
    private String reservationId;
    private String externalOrderId;
    private BigDecimal originalAmount;
    private BigDecimal finalPayableAmount;
    private LocalDateTime expiresAt;
    private String pgOrderId; // PG사에 전달할 주문번호
    
    // 마일리지 관련 정보
    private MileageInfo mileageInfo;
    
    private List<AppliedPromotion> appliedPromotions;
    private List<String> validationErrors;
    
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class MileageInfo {
        private BigDecimal usedMileage;              // 사용된 마일리지
        private BigDecimal mileageDiscount;          // 마일리지 할인 금액 (원화)
        private BigDecimal availableMileage;         // 보유 마일리지
        private BigDecimal maxUsableMileage;         // 최대 사용 가능 마일리지
        private BigDecimal recommendedMileage;       // 권장 사용 마일리지
        private BigDecimal expectedEarning;          // 예상 적립 마일리지
        private BigDecimal usageRate;                // 마일리지 사용률 (%)
        private String usageRateDisplay;             // 사용률 표시용 (예: "15.5%")
    }
    
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class AppliedPromotion {
        private String type;
        private String identifier;
        private String description;
        private BigDecimal discountAmount;
        private BigDecimal pointsUsed;
        private BigDecimal amountDeducted;
        private String status; // APPLIED, FAILED
    }
    
    /**
     * 총 할인 금액 계산
     */
    public BigDecimal getTotalDiscountAmount() {
        if (appliedPromotions == null || appliedPromotions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return appliedPromotions.stream()
            .filter(p -> "APPLIED".equals(p.getStatus()))
            .map(p -> p.getDiscountAmount() != null ? p.getDiscountAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 만료 여부 확인
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * ID 조회 (하위 호환성)
     */
    public String getId() {
        return calculationId;
    }
    
    /**
     * 원래 금액 조회 (별칭)
     */
    public BigDecimal getAmountOriginalTotal() {
        return originalAmount;
    }
    
    /**
     * 총 할인 금액 조회 (별칭)
     */
    public BigDecimal getTotalDiscountAmountApplied() {
        return getTotalDiscountAmount();
    }
} 