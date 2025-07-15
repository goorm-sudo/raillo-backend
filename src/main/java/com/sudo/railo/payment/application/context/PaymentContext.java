package com.sudo.railo.payment.application.context;

import com.sudo.railo.payment.domain.entity.MemberType;
import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.exception.PaymentContextException;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 실행 컨텍스트
 * 
 * 결제 프로세스 전반에서 필요한 모든 정보를 담는 불변 객체입니다.
 * 요청 검증, 계산 정보, 마일리지 검증 결과 등을 통합 관리합니다.
 */
@Builder
@Getter
public class PaymentContext {
    
    private final PaymentExecuteRequest request;
    private final PaymentCalculationResponse calculation;
    private final MileageValidationResult mileageResult;
    private final MemberType memberType;
    private final LocalDateTime createdAt;
    
    /**
     * 회원 결제 여부 확인
     */
    public boolean isForMember() {
        return memberType == MemberType.MEMBER;
    }
    
    /**
     * 마일리지 사용 여부 확인
     */
    public boolean hasMileageUsage() {
        return mileageResult != null && 
               mileageResult.getUsageAmount().compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * 최종 결제 금액 조회
     */
    public BigDecimal getFinalPayableAmount() {
        return calculation.getFinalPayableAmount();
    }
    
    /**
     * Idempotency Key 조회
     */
    public String getIdempotencyKey() {
        return request.getIdempotencyKey();
    }
    
    /**
     * 예약 ID 조회
     */
    public Long getReservationId() {
        String reservationIdStr = calculation.getReservationId();
        if (reservationIdStr == null) {
            return null;
        }
        
        // "R2025060100001" 형태인 경우 'R' 제거 후 숫자 추출
        if (reservationIdStr.startsWith("R")) {
            return Long.parseLong(reservationIdStr.substring(1));
        }
        
        // 이미 숫자 형태인 경우
        return Long.parseLong(reservationIdStr);
    }
    
    /**
     * 회원 ID 조회 (회원인 경우만)
     */
    public Long getMemberId() {
        return isForMember() ? request.getMemberId() : null;
    }
    
    /**
     * 컨텍스트 유효성 검증
     */
    public void validate() {
        if (request == null) {
            throw new PaymentContextException("결제 요청이 없습니다");
        }
        
        if (calculation == null) {
            throw new PaymentContextException("결제 계산 정보가 없습니다");
        }
        
        if (calculation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new PaymentContextException("결제 계산이 만료되었습니다");
        }
        
        if (memberType == null) {
            throw new PaymentContextException("회원 타입이 지정되지 않았습니다");
        }
        
        // 마일리지 사용 시 검증
        if (hasMileageUsage() && !isForMember()) {
            throw new PaymentContextException("비회원은 마일리지를 사용할 수 없습니다");
        }
    }
    
    /**
     * 마일리지 검증 결과
     */
    @Builder
    @Getter
    public static class MileageValidationResult {
        private final BigDecimal availableAmount;
        private final BigDecimal usageAmount;
        private final BigDecimal remainingAmount;
        private final boolean isValid;
        private final String validationMessage;
        
        /**
         * 성공적인 검증 결과 생성
         */
        public static MileageValidationResult success(
                BigDecimal availableAmount, 
                BigDecimal usageAmount) {
            return MileageValidationResult.builder()
                    .availableAmount(availableAmount)
                    .usageAmount(usageAmount)
                    .remainingAmount(availableAmount.subtract(usageAmount))
                    .isValid(true)
                    .validationMessage("마일리지 검증 성공")
                    .build();
        }
        
        /**
         * 실패한 검증 결과 생성
         */
        public static MileageValidationResult failure(String message) {
            return MileageValidationResult.builder()
                    .availableAmount(BigDecimal.ZERO)
                    .usageAmount(BigDecimal.ZERO)
                    .remainingAmount(BigDecimal.ZERO)
                    .isValid(false)
                    .validationMessage(message)
                    .build();
        }
    }
}