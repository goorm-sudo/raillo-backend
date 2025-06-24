package com.sudo.railo.payment.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 마일리지 도메인 서비스
 * 
 * 비즈니스 규칙:
 * - 1포인트 = 1원
 * - 최대 사용: 결제 금액의 30%
 * - 최소 사용: 1,000포인트 이상
 * - 사용 단위: 100포인트 단위
 * - 적립률: 결제 금액의 1%
 * - 유효기간: 2년
 */
@Service
@Slf4j
public class MileageService {

    // 비즈니스 상수
    private static final BigDecimal MAX_USAGE_RATE = new BigDecimal("0.30"); // 30%
    private static final BigDecimal MIN_USAGE_AMOUNT = new BigDecimal("1000"); // 1,000포인트
    private static final BigDecimal USAGE_UNIT = new BigDecimal("100"); // 100포인트 단위
    private static final BigDecimal EARNING_RATE = new BigDecimal("0.01"); // 1%
    private static final BigDecimal MILEAGE_TO_WON_RATE = BigDecimal.ONE; // 1포인트 = 1원

    /**
     * 마일리지 사용 가능 여부 검증
     * 
     * @param requestedMileage 사용 요청 마일리지
     * @param availableMileage 보유 마일리지
     * @param paymentAmount 결제 금액
     * @return 검증 결과
     */
    public boolean validateMileageUsage(BigDecimal requestedMileage, BigDecimal availableMileage, BigDecimal paymentAmount) {
        log.debug("마일리지 사용 검증 시작 - 요청: {}, 보유: {}, 결제금액: {}", 
                requestedMileage, availableMileage, paymentAmount);

        // 1. 요청 마일리지가 0 이하인 경우
        if (requestedMileage.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("마일리지 사용 요청이 0 이하입니다: {}", requestedMileage);
            return true; // 사용하지 않는 경우는 유효
        }

        // 2. 보유 마일리지 부족 검증
        if (requestedMileage.compareTo(availableMileage) > 0) {
            log.warn("보유 마일리지 부족 - 요청: {}, 보유: {}", requestedMileage, availableMileage);
            return false;
        }

        // 3. 최소 사용 금액 검증
        if (requestedMileage.compareTo(MIN_USAGE_AMOUNT) < 0) {
            log.warn("최소 사용 금액 미달 - 요청: {}, 최소: {}", requestedMileage, MIN_USAGE_AMOUNT);
            return false;
        }

        // 4. 사용 단위 검증 (100포인트 단위)
        if (requestedMileage.remainder(USAGE_UNIT).compareTo(BigDecimal.ZERO) != 0) {
            log.warn("사용 단위 오류 - 요청: {}, 단위: {}", requestedMileage, USAGE_UNIT);
            return false;
        }

        // 5. 최대 사용 한도 검증 (결제 금액의 30%)
        BigDecimal maxUsableAmount = calculateMaxUsableAmount(paymentAmount);
        if (requestedMileage.compareTo(maxUsableAmount) > 0) {
            log.warn("최대 사용 한도 초과 - 요청: {}, 최대: {}", requestedMileage, maxUsableAmount);
            return false;
        }

        log.debug("마일리지 사용 검증 성공");
        return true;
    }

    /**
     * 최대 사용 가능한 마일리지 계산
     * 
     * @param paymentAmount 결제 금액
     * @return 최대 사용 가능 마일리지
     */
    public BigDecimal calculateMaxUsableAmount(BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxUsable = paymentAmount.multiply(MAX_USAGE_RATE)
                .setScale(0, RoundingMode.DOWN); // 소수점 버림

        // 100포인트 단위로 내림
        BigDecimal remainder = maxUsable.remainder(USAGE_UNIT);
        maxUsable = maxUsable.subtract(remainder);

        log.debug("최대 사용 가능 마일리지 계산 - 결제금액: {}, 최대사용: {}", paymentAmount, maxUsable);
        return maxUsable;
    }

    /**
     * 마일리지를 원화로 변환
     * 
     * @param mileageAmount 마일리지 금액
     * @return 원화 금액
     */
    public BigDecimal convertMileageToWon(BigDecimal mileageAmount) {
        if (mileageAmount == null || mileageAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal wonAmount = mileageAmount.multiply(MILEAGE_TO_WON_RATE);
        log.debug("마일리지 원화 변환 - 마일리지: {}, 원화: {}", mileageAmount, wonAmount);
        return wonAmount;
    }

    /**
     * 원화를 마일리지로 변환
     * 
     * @param wonAmount 원화 금액
     * @return 마일리지 금액
     */
    public BigDecimal convertWonToMileage(BigDecimal wonAmount) {
        if (wonAmount == null || wonAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal mileageAmount = wonAmount.divide(MILEAGE_TO_WON_RATE, 0, RoundingMode.DOWN);
        log.debug("원화 마일리지 변환 - 원화: {}, 마일리지: {}", wonAmount, mileageAmount);
        return mileageAmount;
    }

    /**
     * 마일리지 적립 금액 계산
     * 
     * @param paymentAmount 결제 금액
     * @return 적립될 마일리지
     */
    public BigDecimal calculateEarningAmount(BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal earningAmount = paymentAmount.multiply(EARNING_RATE)
                .setScale(0, RoundingMode.DOWN); // 소수점 버림

        log.debug("마일리지 적립 계산 - 결제금액: {}, 적립: {}", paymentAmount, earningAmount);
        return earningAmount;
    }

    /**
     * 마일리지 사용 후 최종 결제 금액 계산
     * 
     * @param originalAmount 원래 결제 금액
     * @param usedMileage 사용한 마일리지
     * @return 최종 결제 금액
     */
    public BigDecimal calculateFinalAmount(BigDecimal originalAmount, BigDecimal usedMileage) {
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (usedMileage == null || usedMileage.compareTo(BigDecimal.ZERO) < 0) {
            usedMileage = BigDecimal.ZERO;
        }

        BigDecimal mileageDiscount = convertMileageToWon(usedMileage);
        BigDecimal finalAmount = originalAmount.subtract(mileageDiscount);

        // 최종 금액이 음수가 되지 않도록 보장
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        log.debug("최종 결제 금액 계산 - 원금액: {}, 마일리지할인: {}, 최종금액: {}", 
                originalAmount, mileageDiscount, finalAmount);
        return finalAmount;
    }

    /**
     * 마일리지 사용 권장 금액 계산
     * 보유 마일리지와 최대 사용 가능 금액 중 작은 값을 100포인트 단위로 계산
     * 
     * @param availableMileage 보유 마일리지
     * @param paymentAmount 결제 금액
     * @return 권장 사용 마일리지
     */
    public BigDecimal calculateRecommendedUsage(BigDecimal availableMileage, BigDecimal paymentAmount) {
        if (availableMileage == null || availableMileage.compareTo(MIN_USAGE_AMOUNT) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxUsable = calculateMaxUsableAmount(paymentAmount);
        BigDecimal recommended = availableMileage.min(maxUsable);

        // 100포인트 단위로 내림
        BigDecimal remainder = recommended.remainder(USAGE_UNIT);
        recommended = recommended.subtract(remainder);

        // 최소 사용 금액 미달시 0 반환
        if (recommended.compareTo(MIN_USAGE_AMOUNT) < 0) {
            recommended = BigDecimal.ZERO;
        }

        log.debug("권장 마일리지 사용량 계산 - 보유: {}, 권장: {}", availableMileage, recommended);
        return recommended;
    }

    /**
     * 마일리지 사용률 계산
     * 
     * @param usedMileage 사용한 마일리지
     * @param paymentAmount 결제 금액
     * @return 사용률 (0.0 ~ 1.0)
     */
    public BigDecimal calculateUsageRate(BigDecimal usedMileage, BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (usedMileage == null || usedMileage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal mileageWon = convertMileageToWon(usedMileage);
        BigDecimal usageRate = mileageWon.divide(paymentAmount, 4, RoundingMode.HALF_UP);

        log.debug("마일리지 사용률 계산 - 사용마일리지: {}, 결제금액: {}, 사용률: {}", 
                usedMileage, paymentAmount, usageRate);
        return usageRate;
    }
} 