package com.sudo.railo.payment.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 마일리지 도메인 서비스
 * 1포인트 = 1원, 최대 100%, 최소 1,000포인트, 1포인트 단위
 */
@Service
@Slf4j
public class MileageService {

    private static final BigDecimal MAX_USAGE_RATE = new BigDecimal("1.00");
    private static final BigDecimal MIN_USAGE_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal USAGE_UNIT = new BigDecimal("1");
    private static final BigDecimal EARNING_RATE = new BigDecimal("0.01");
    private static final BigDecimal MILEAGE_TO_WON_RATE = BigDecimal.ONE;

    public boolean validateMileageUsage(BigDecimal requestedMileage, BigDecimal availableMileage, BigDecimal paymentAmount) {
        if (requestedMileage.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }

        if (requestedMileage.compareTo(availableMileage) > 0) {
            log.debug("보유 마일리지 부족 - 요청: {}, 보유: {}", requestedMileage, availableMileage);
            return false;
        }

        if (requestedMileage.compareTo(MIN_USAGE_AMOUNT) < 0) {
            return false;
        }

        if (requestedMileage.remainder(USAGE_UNIT).compareTo(BigDecimal.ZERO) != 0) {
            return false;
        }

        BigDecimal maxUsableAmount = calculateMaxUsableAmount(paymentAmount);
        if (requestedMileage.compareTo(maxUsableAmount) > 0) {
            return false;
        }

        return true;
    }

    public BigDecimal calculateMaxUsableAmount(BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxUsable = paymentAmount.multiply(MAX_USAGE_RATE)
                .setScale(0, RoundingMode.DOWN);

        BigDecimal remainder = maxUsable.remainder(USAGE_UNIT);
        return maxUsable.subtract(remainder);
    }

    public BigDecimal convertMileageToWon(BigDecimal mileageAmount) {
        if (mileageAmount == null || mileageAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return mileageAmount.multiply(MILEAGE_TO_WON_RATE);
    }

    public BigDecimal convertWonToMileage(BigDecimal wonAmount) {
        if (wonAmount == null || wonAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return wonAmount.divide(MILEAGE_TO_WON_RATE, 0, RoundingMode.DOWN);
    }

    public BigDecimal calculateEarningAmount(BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return paymentAmount.multiply(EARNING_RATE).setScale(0, RoundingMode.DOWN);
    }

    public BigDecimal calculateFinalAmount(BigDecimal originalAmount, BigDecimal usedMileage) {
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (usedMileage == null || usedMileage.compareTo(BigDecimal.ZERO) < 0) {
            usedMileage = BigDecimal.ZERO;
        }

        BigDecimal mileageDiscount = convertMileageToWon(usedMileage);
        BigDecimal finalAmount = originalAmount.subtract(mileageDiscount);

        return finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount;
    }

    public BigDecimal calculateRecommendedUsage(BigDecimal availableMileage, BigDecimal paymentAmount) {
        if (availableMileage == null || availableMileage.compareTo(MIN_USAGE_AMOUNT) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxUsable = calculateMaxUsableAmount(paymentAmount);
        BigDecimal recommended = availableMileage.min(maxUsable);

        BigDecimal remainder = recommended.remainder(USAGE_UNIT);
        recommended = recommended.subtract(remainder);

        return recommended.compareTo(MIN_USAGE_AMOUNT) < 0 ? BigDecimal.ZERO : recommended;
    }

    public BigDecimal calculateUsageRate(BigDecimal usedMileage, BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0 ||
            usedMileage == null || usedMileage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal mileageWon = convertMileageToWon(usedMileage);
        return mileageWon.divide(paymentAmount, 4, RoundingMode.HALF_UP);
    }
} 