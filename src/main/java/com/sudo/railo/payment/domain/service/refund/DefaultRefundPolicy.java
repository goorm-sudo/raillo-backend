package com.sudo.railo.payment.domain.service.refund;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 기본 환불 정책 구현
 * 운영사가 명시되지 않았거나 매칭되는 정책이 없을 때 사용
 * 기존 하드코딩된 로직과 동일한 정책 적용
 */
@Slf4j
@Component
public class DefaultRefundPolicy implements RefundPolicyService {
    
    private final KorailRefundPolicy korailPolicy;
    
    public DefaultRefundPolicy() {
        // 기본 정책은 KORAIL 정책을 따름
        this.korailPolicy = new KorailRefundPolicy();
    }
    
    @Override
    public BigDecimal calculateRefundFeeRate(LocalDateTime departureTime, 
                                             LocalDateTime arrivalTime, 
                                             LocalDateTime requestTime) {
        log.warn("기본 환불 정책 사용 - KORAIL 정책으로 대체");
        return korailPolicy.calculateRefundFeeRate(departureTime, arrivalTime, requestTime);
    }
    
    @Override
    public boolean supports(String operator) {
        // 기본 정책은 모든 경우를 지원 (fallback)
        return true;
    }
    
    @Override
    public String getPolicyName() {
        return "기본 환불 정책 (KORAIL 정책 준용)";
    }
}