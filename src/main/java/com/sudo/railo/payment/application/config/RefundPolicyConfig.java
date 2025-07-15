package com.sudo.railo.payment.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 환불 정책 설정
 * application.yml에서 환불 수수료율을 설정할 수 있도록 지원
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "payment.refund")
public class RefundPolicyConfig {
    
    /**
     * 운영사별 환불 정책 설정
     */
    private Map<String, OperatorRefundPolicy> operators = new HashMap<>();
    
    /**
     * 기본 환불 정책 활성화 여부
     */
    private boolean defaultPolicyEnabled = true;
    
    @Data
    public static class OperatorRefundPolicy {
        /**
         * 정책 활성화 여부
         */
        private boolean enabled = true;
        
        /**
         * 출발 전 환불 수수료율
         */
        private Map<String, BigDecimal> beforeDeparture = new HashMap<>();
        
        /**
         * 출발 후 환불 수수료율
         */
        private Map<String, BigDecimal> afterDeparture = new HashMap<>();
        
        /**
         * 도착 후 환불 가능 여부
         */
        private boolean refundableAfterArrival = false;
    }
}