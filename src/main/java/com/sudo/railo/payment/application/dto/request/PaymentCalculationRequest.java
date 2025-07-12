package com.sudo.railo.payment.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import com.sudo.railo.train.domain.type.TrainOperator;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentCalculationRequest {
    
    // 예약 ID (Optional - 예약 삭제 시에도 결제 가능하도록)
    private Long reservationId;
    
    @NotBlank(message = "주문 ID는 필수입니다")
    private String externalOrderId;
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @NotNull(message = "원본 금액은 필수입니다")
    @DecimalMin(value = "0", message = "금액은 0 이상이어야 합니다")
    private BigDecimal originalAmount;
    
    @Valid
    private List<PaymentItem> items;
    
    @Valid
    private List<PromotionRequest> requestedPromotions;
    
    @DecimalMin(value = "0", message = "마일리지 사용 금액은 0 이상이어야 합니다")
    @Builder.Default
    private BigDecimal mileageToUse = BigDecimal.ZERO;
    
    @DecimalMin(value = "0", message = "보유 마일리지는 0 이상이어야 합니다")
    @Builder.Default
    private BigDecimal availableMileage = BigDecimal.ZERO;
    
    private String clientIp;
    private String userAgent;
    
    // 열차 정보 (예약 삭제 시에도 결제 가능하도록 직접 전달)
    private Long trainScheduleId;
    private LocalDateTime trainDepartureTime;
    private LocalDateTime trainArrivalTime;
    private TrainOperator trainOperator;
    private String routeInfo; // 예: "서울-부산"
    private String seatNumber; // 좌석 번호
    
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PaymentItem {
        @NotBlank(message = "상품 ID는 필수입니다")
        private String productId;
        
        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        private Integer quantity;
        
        @DecimalMin(value = "0", message = "단가는 0 이상이어야 합니다")
        private BigDecimal unitPrice;
    }
    
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PromotionRequest {
        @NotBlank(message = "프로모션 타입은 필수입니다")
        private String type; // COUPON, MILEAGE, DISCOUNT_CODE
        
        private String identifier; // 쿠폰 코드 등
        private BigDecimal pointsToUse; // 마일리지 사용 포인트
    }
}