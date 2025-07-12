package com.sudo.railo.payment.domain.service;

import com.sudo.railo.payment.domain.entity.RefundCalculation;
import com.sudo.railo.payment.domain.entity.RefundStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 환불 계산 관련 도메인 서비스
 * RefundCalculation 엔티티의 비즈니스 로직을 분리
 */
@Slf4j
@Service
public class RefundCalculationService {
    
    /**
     * 환불 가능 여부 확인 (시간 기준)
     * 
     * @param calculation 환불 계산 정보
     * @return 환불 가능 여부
     */
    public boolean isRefundableByTime(RefundCalculation calculation) {
        if (calculation == null) {
            log.warn("RefundCalculation이 null입니다.");
            return false;
        }
        
        return isRefundableByTime(
            calculation.getTrainArrivalTime(),
            calculation.getRefundStatus()
        );
    }
    
    /**
     * 환불 가능 여부 확인 (시간 기준)
     * 
     * @param trainArrivalTime 열차 도착 시간
     * @param refundStatus 환불 상태
     * @return 환불 가능 여부
     */
    public boolean isRefundableByTime(LocalDateTime trainArrivalTime, RefundStatus refundStatus) {
        // 이미 환불 완료된 경우
        if (refundStatus == RefundStatus.COMPLETED) {
            log.info("이미 환불 완료된 상태입니다.");
            return false;
        }
        
        // 도착 시간이 없는 경우 (데이터 이상)
        if (trainArrivalTime == null) {
            log.warn("열차 도착 시간이 null입니다. 환불 불가로 처리합니다.");
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        boolean isRefundable = now.isBefore(trainArrivalTime);
        
        log.debug("환불 가능 여부 확인 - 현재시간: {}, 도착시간: {}, 환불가능: {}", 
                 now, trainArrivalTime, isRefundable);
        
        return isRefundable;
    }
    
    /**
     * 환불 가능 시간 확인 (Optional 반환)
     * 
     * @param trainArrivalTime 열차 도착 시간
     * @param refundStatus 환불 상태
     * @return 환불 가능 여부 (Optional)
     */
    public Optional<Boolean> checkRefundability(LocalDateTime trainArrivalTime, RefundStatus refundStatus) {
        if (trainArrivalTime == null) {
            return Optional.empty();
        }
        
        return Optional.of(isRefundableByTime(trainArrivalTime, refundStatus));
    }
    
    /**
     * 환불 수수료율 유효성 검증
     * 
     * @param refundFeeRate 환불 수수료율
     * @return 유효성 여부
     */
    public boolean isValidRefundFeeRate(BigDecimal refundFeeRate) {
        if (refundFeeRate == null) {
            return false;
        }
        
        // 0% ~ 100% 사이인지 확인
        return refundFeeRate.compareTo(BigDecimal.ZERO) >= 0 
            && refundFeeRate.compareTo(BigDecimal.ONE) <= 0;
    }
    
    /**
     * 환불 금액 계산
     * 
     * @param originalAmount 원본 금액
     * @param refundFeeRate 환불 수수료율
     * @return 환불 금액
     */
    public BigDecimal calculateRefundAmount(BigDecimal originalAmount, BigDecimal refundFeeRate) {
        if (originalAmount == null || refundFeeRate == null) {
            throw new IllegalArgumentException("금액과 수수료율은 필수입니다.");
        }
        
        if (!isValidRefundFeeRate(refundFeeRate)) {
            throw new IllegalArgumentException("유효하지 않은 환불 수수료율입니다: " + refundFeeRate);
        }
        
        BigDecimal refundFee = originalAmount.multiply(refundFeeRate);
        return originalAmount.subtract(refundFee);
    }
    
    /**
     * 환불 수수료 계산
     * 
     * @param originalAmount 원본 금액
     * @param refundFeeRate 환불 수수료율
     * @return 환불 수수료
     */
    public BigDecimal calculateRefundFee(BigDecimal originalAmount, BigDecimal refundFeeRate) {
        if (originalAmount == null || refundFeeRate == null) {
            throw new IllegalArgumentException("금액과 수수료율은 필수입니다.");
        }
        
        if (!isValidRefundFeeRate(refundFeeRate)) {
            throw new IllegalArgumentException("유효하지 않은 환불 수수료율입니다: " + refundFeeRate);
        }
        
        return originalAmount.multiply(refundFeeRate);
    }
}