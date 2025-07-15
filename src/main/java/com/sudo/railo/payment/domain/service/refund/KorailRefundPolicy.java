package com.sudo.railo.payment.domain.service.refund;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 한국철도공사(KORAIL) 환불 정책 구현
 * KTX, KTX-산천, KTX-청룡, KTX-이음 등에 적용
 */
@Slf4j
@Component
public class KorailRefundPolicy implements RefundPolicyService {
    
    // 환불 수수료율 상수
    private static final BigDecimal NO_FEE = BigDecimal.ZERO;
    private static final BigDecimal FEE_30_PERCENT = new BigDecimal("0.3");
    private static final BigDecimal FEE_40_PERCENT = new BigDecimal("0.4");
    private static final BigDecimal FEE_70_PERCENT = new BigDecimal("0.7");
    private static final BigDecimal FULL_FEE = BigDecimal.ONE;
    
    // 시간 기준 상수 (분)
    private static final long MINUTES_20 = 20;
    private static final long MINUTES_60 = 60;
    
    @Override
    public BigDecimal calculateRefundFeeRate(LocalDateTime departureTime, 
                                             LocalDateTime arrivalTime, 
                                             LocalDateTime requestTime) {
        log.debug("KORAIL 환불 수수료 계산 - 출발: {}, 도착: {}, 요청: {}", 
                  departureTime, arrivalTime, requestTime);
        
        // 1. 도착 후 환불 요청 - 환불 불가 (100% 수수료)
        if (requestTime.isAfter(arrivalTime)) {
            log.info("도착 후 환불 요청 - 환불 불가");
            return FULL_FEE;
        }
        
        // 2. 출발 전 환불 - 수수료 없음
        if (requestTime.isBefore(departureTime)) {
            log.info("출발 전 환불 요청 - 수수료 없음");
            return NO_FEE;
        }
        
        // 3. 출발 후 ~ 도착 전 환불 - 경과 시간에 따른 수수료
        long minutesAfterDeparture = Duration.between(departureTime, requestTime).toMinutes();
        
        BigDecimal feeRate;
        if (minutesAfterDeparture <= MINUTES_20) {
            feeRate = FEE_30_PERCENT;
            log.info("출발 후 {}분 이내 환불 - 30% 수수료", minutesAfterDeparture);
        } else if (minutesAfterDeparture <= MINUTES_60) {
            feeRate = FEE_40_PERCENT;
            log.info("출발 후 {}분 이내 환불 - 40% 수수료", minutesAfterDeparture);
        } else {
            feeRate = FEE_70_PERCENT;
            log.info("출발 후 {}분 초과 환불 - 70% 수수료", minutesAfterDeparture);
        }
        
        return feeRate;
    }
    
    @Override
    public boolean supports(String operator) {
        return "KORAIL".equalsIgnoreCase(operator);
    }
    
    @Override
    public String getPolicyName() {
        return "KORAIL 표준 환불 정책";
    }
}