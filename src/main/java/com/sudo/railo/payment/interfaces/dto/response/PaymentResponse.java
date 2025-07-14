package com.sudo.railo.payment.interfaces.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 확인 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PaymentResponse {
    
    private Long paymentId;
    private String status;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime completedAt;
    private String pgTransactionId;
    private String pgApprovalNumber;
    
    // 성공 응답 생성
    public static PaymentResponse success(Long paymentId) {
        return PaymentResponse.builder()
            .paymentId(paymentId)
            .status("SUCCESS")
            .completedAt(LocalDateTime.now())
            .build();
    }
    
    // 실패 응답 생성
    public static PaymentResponse fail(String reason) {
        return PaymentResponse.builder()
            .status("FAILED")
            .completedAt(LocalDateTime.now())
            .build();
    }
}