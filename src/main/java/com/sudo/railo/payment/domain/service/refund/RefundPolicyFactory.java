package com.sudo.railo.payment.domain.service.refund;

import com.sudo.railo.train.domain.type.TrainOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 환불 정책 팩토리
 * 운영사에 따라 적절한 환불 정책을 선택하여 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundPolicyFactory {
    
    private final List<RefundPolicyService> policies;
    private final DefaultRefundPolicy defaultPolicy;
    
    /**
     * 운영사에 맞는 환불 정책 조회
     * 
     * @param operator 운영사
     * @return 환불 정책 서비스
     */
    public RefundPolicyService getPolicy(TrainOperator operator) {
        return getPolicy(operator.name());
    }
    
    /**
     * 운영사 이름으로 환불 정책 조회
     * 
     * @param operatorName 운영사 이름
     * @return 환불 정책 서비스
     */
    public RefundPolicyService getPolicy(String operatorName) {
        log.debug("환불 정책 조회 - 운영사: {}", operatorName);
        
        if (operatorName == null) {
            log.warn("운영사 정보가 없습니다. 기본 정책을 사용합니다.");
            return defaultPolicy;
        }
        
        return policies.stream()
                .filter(policy -> policy.supports(operatorName))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("운영사 {}에 대한 환불 정책을 찾을 수 없습니다. 기본 정책을 사용합니다.", operatorName);
                    return defaultPolicy;
                });
    }
    
    /**
     * 기본 환불 정책 조회
     * 
     * @return 기본 환불 정책 서비스
     */
    public RefundPolicyService getDefaultPolicy() {
        return defaultPolicy;
    }
    
    /**
     * 등록된 모든 정책 목록 조회
     * 
     * @return 환불 정책 목록
     */
    public List<RefundPolicyService> getAllPolicies() {
        return policies;
    }
}