package com.sudo.railo.payment.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentExecuteRequest {
    
    @NotBlank(message = "계산 세션 ID는 필수입니다")
    private String calculationId;
    
    @Valid
    @NotNull(message = "결제 수단 정보는 필수입니다")
    private PaymentMethodInfo paymentMethod;
    
    @NotBlank(message = "중복 방지 키는 필수입니다")
    private String idempotencyKey;
    
    // 회원 정보
    private Long memberId;
    
    // 비회원 정보 (회원 ID가 없을 경우 필수)
    private String nonMemberName;
    private String nonMemberPhone;
    private String nonMemberPassword;
    
    // 마일리지 정보 (회원인 경우만 사용)
    @Builder.Default
    private BigDecimal mileageToUse = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal availableMileage = BigDecimal.ZERO;
    
    // 현금영수증 정보
    @Builder.Default
    private Boolean requestReceipt = false; // 현금영수증 신청 여부
    
    private String receiptType; // 현금영수증 타입: "personal" 또는 "business"
    private String receiptPhoneNumber; // 개인 소득공제용 휴대폰 번호
    private String businessNumber; // 사업자 증빙용 사업자등록번호
    
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PaymentMethodInfo {
        @NotBlank(message = "결제 타입은 필수입니다")
        private String type; // CREDIT_CARD, BANK_TRANSFER, MOBILE
        
        @NotBlank(message = "PG 제공자는 필수입니다")
        private String pgProvider; // TOSS_PAYMENTS, IAMPORT, etc.
        
        @NotBlank(message = "PG 토큰은 필수입니다")
        private String pgToken;
        
        private Map<String, Object> additionalInfo;
    }
} 