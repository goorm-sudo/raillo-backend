package com.sudo.railo.payment.domain.service;

import com.sudo.railo.payment.application.dto.request.PaymentCalculationRequest;
import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentValidationService {
    
    public void validateCalculationRequest(PaymentCalculationRequest request) {
        List<String> errors = new ArrayList<>();
        
        // 기본 필드 검증
        if (request.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("결제 금액은 0보다 커야 합니다");
        }
        
        if (request.getOriginalAmount().compareTo(BigDecimal.valueOf(10000000)) > 0) {
            errors.add("결제 금액은 1천만원을 초과할 수 없습니다");
        }
        
        // 프로모션 검증
        if (request.getRequestedPromotions() != null) {
            validatePromotions(request.getRequestedPromotions(), request.getOriginalAmount(), errors);
        }
        
        if (!errors.isEmpty()) {
            throw new PaymentValidationException("결제 계산 요청 검증 실패: " + String.join(", ", errors));
        }
    }
    
    public void validateExecuteRequest(PaymentExecuteRequest request) {
        List<String> errors = new ArrayList<>();
        
        // 계산 세션 ID 검증
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            errors.add("계산 세션 ID는 필수입니다");
        }
        
        // 중복 방지 키 검증
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().trim().isEmpty()) {
            errors.add("중복 방지 키는 필수입니다");
        }
        
        // 결제 수단 검증
        if (request.getPaymentMethod() != null) {
            validatePaymentMethod(request.getPaymentMethod(), errors);
        }
        
        if (!errors.isEmpty()) {
            throw new PaymentValidationException("결제 실행 요청 검증 실패: " + String.join(", ", errors));
        }
    }
    
    private void validatePromotions(List<PaymentCalculationRequest.PromotionRequest> promotions, 
            BigDecimal originalAmount, List<String> errors) {
        
        for (PaymentCalculationRequest.PromotionRequest promotion : promotions) {
            if ("MILEAGE".equals(promotion.getType())) {
                if (promotion.getPointsToUse() == null || 
                    promotion.getPointsToUse().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("마일리지 사용 포인트는 0보다 커야 합니다");
                }
                
                if (promotion.getPointsToUse() != null && 
                    promotion.getPointsToUse().compareTo(originalAmount) > 0) {
                    errors.add("마일리지 사용 포인트는 결제 금액을 초과할 수 없습니다");
                }
            }
        }
    }
    
    private void validatePaymentMethod(PaymentExecuteRequest.PaymentMethodInfo paymentMethod, 
            List<String> errors) {
        
        if (paymentMethod.getType() == null || paymentMethod.getType().trim().isEmpty()) {
            errors.add("결제 수단 타입은 필수입니다");
        }
        
        if (paymentMethod.getPgProvider() == null || paymentMethod.getPgProvider().trim().isEmpty()) {
            errors.add("PG 제공자는 필수입니다");
        }
        
        if (paymentMethod.getPgToken() == null || paymentMethod.getPgToken().trim().isEmpty()) {
            errors.add("PG 토큰은 필수입니다");
        }
    }
} 