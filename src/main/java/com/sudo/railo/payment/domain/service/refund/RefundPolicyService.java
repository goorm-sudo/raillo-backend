package com.sudo.railo.payment.domain.service.refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환불 정책 서비스 인터페이스
 * 운영사별로 다른 환불 수수료 정책을 적용하기 위한 인터페이스
 */
public interface RefundPolicyService {
    
    /**
     * 환불 수수료율 계산
     * 
     * @param departureTime 열차 출발 시간
     * @param arrivalTime 열차 도착 시간  
     * @param requestTime 환불 요청 시간
     * @return 환불 수수료율 (0.0 ~ 1.0)
     */
    BigDecimal calculateRefundFeeRate(LocalDateTime departureTime, 
                                      LocalDateTime arrivalTime, 
                                      LocalDateTime requestTime);
    
    /**
     * 환불 가능 여부 확인
     * 
     * @param arrivalTime 열차 도착 시간
     * @param requestTime 환불 요청 시간
     * @return 환불 가능하면 true
     */
    default boolean isRefundable(LocalDateTime arrivalTime, LocalDateTime requestTime) {
        return requestTime.isBefore(arrivalTime);
    }
    
    /**
     * 이 정책이 지원하는 운영사인지 확인
     * 
     * @param operator 운영사
     * @return 지원하면 true
     */
    boolean supports(String operator);
    
    /**
     * 정책 이름 조회
     * 
     * @return 정책 이름
     */
    String getPolicyName();
}