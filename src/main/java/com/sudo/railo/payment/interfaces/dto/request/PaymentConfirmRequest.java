package com.sudo.railo.payment.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * PG 결제 확인 요청 DTO
 * 계산 세션 ID와 PG 승인번호로 결제를 최종 확인
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PaymentConfirmRequest {
    
    @NotBlank(message = "계산 ID는 필수입니다")
    private String calculationId;
    
    @NotBlank(message = "PG 승인번호는 필수입니다")
    private String pgAuthNumber;
    
    // PG사에서 반환한 거래 ID (선택)
    private String pgTransactionId;
    
    // 추가 검증을 위한 정보 (선택)
    private String paymentMethod; // CREDIT_CARD, KAKAO_PAY 등
    
    // 비회원인 경우 추가 정보
    private String nonMemberName;
    private String nonMemberPhone;
    private String nonMemberPassword;
}