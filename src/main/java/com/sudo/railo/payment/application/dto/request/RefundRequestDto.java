package com.sudo.railo.payment.application.dto.request;

import com.sudo.railo.payment.domain.entity.RefundType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 환불 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDto {
    
    private String idempotencyKey;  // 멱등성 키 (선택적, 클라이언트에서 생성)
    
    @NotNull(message = "결제 ID는 필수입니다")
    private Long paymentId;
    
    @NotNull(message = "환불 유형은 필수입니다")
    private RefundType refundType;
    
    private LocalDateTime trainDepartureTime;
    
    private LocalDateTime trainArrivalTime;
    
    private String refundReason;
    
    /**
     * ID 조회 (하위 호환성)
     */
    public Long getId() {
        return paymentId;
    }
} 