package com.sudo.railo.payment.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * 마일리지 도메인 서비스 테스트
 * 
 * 테스트 범위:
 * - 마일리지 사용 검증 (비즈니스 규칙)
 * - 마일리지 계산 로직 (변환, 할인, 적립)
 * - 경계값 및 예외 상황 처리
 * - 비즈니스 상수 검증
 */
@DisplayName("마일리지 도메인 서비스 테스트")
class MileageServiceTest {

    private MileageService mileageService;

    @BeforeEach
    void setUp() {
        mileageService = new MileageService();
    }

    @Nested
    @DisplayName("마일리지 사용 검증 테스트")
    class ValidateMileageUsageTest {

        @Test
        @DisplayName("정상적인 마일리지 사용 - 검증 성공")
        void validateMileageUsage_ValidRequest_ReturnsTrue() {
            // given
            BigDecimal requestedMileage = new BigDecimal("5000");
            BigDecimal availableMileage = new BigDecimal("10000");
            BigDecimal paymentAmount = new BigDecimal("20000");

            // when
            boolean result = mileageService.validateMileageUsage(requestedMileage, availableMileage, paymentAmount);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("보유 마일리지 부족 - 검증 실패")
        void validateMileageUsage_InsufficientMileage_ReturnsFalse() {
            // given
            BigDecimal requestedMileage = new BigDecimal("15000");
            BigDecimal availableMileage = new BigDecimal("10000");
            BigDecimal paymentAmount = new BigDecimal("50000");

            // when
            boolean result = mileageService.validateMileageUsage(requestedMileage, availableMileage, paymentAmount);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("최소 사용 금액 미달 - 검증 실패")
        void validateMileageUsage_BelowMinimumAmount_ReturnsFalse() {
            // given
            BigDecimal requestedMileage = new BigDecimal("500");
            BigDecimal availableMileage = new BigDecimal("10000");
            BigDecimal paymentAmount = new BigDecimal("20000");

            // when
            boolean result = mileageService.validateMileageUsage(requestedMileage, availableMileage, paymentAmount);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("최대 사용 한도 초과 - 검증 실패")
        void validateMileageUsage_ExceedsMaxUsage_ReturnsFalse() {
            // given
            BigDecimal requestedMileage = new BigDecimal("7000");
            BigDecimal availableMileage = new BigDecimal("10000");
            BigDecimal paymentAmount = new BigDecimal("20000");

            // when
            boolean result = mileageService.validateMileageUsage(requestedMileage, availableMileage, paymentAmount);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("최대 사용 가능 마일리지 계산 테스트")
    class CalculateMaxUsableAmountTest {

        @ParameterizedTest
        @CsvSource({
            "10000, 3000",
            "20000, 6000",
            "50000, 15000"
        })
        @DisplayName("정상적인 최대 사용 가능 마일리지 계산")
        void calculateMaxUsableAmount_ValidAmount_ReturnsCorrectValue(String paymentAmount, String expectedMax) {
            // given
            BigDecimal amount = new BigDecimal(paymentAmount);
            BigDecimal expected = new BigDecimal(expectedMax);

            // when
            BigDecimal result = mileageService.calculateMaxUsableAmount(amount);

            // then
            assertThat(result).isEqualByComparingTo(expected);
        }
    }

    @Nested
    @DisplayName("마일리지 적립 계산 테스트")
    class CalculateEarningAmountTest {

        @ParameterizedTest
        @CsvSource({
            "10000, 100",
            "50000, 500",
            "12345, 123"
        })
        @DisplayName("정상적인 마일리지 적립 계산")
        void calculateEarningAmount_ValidAmount_ReturnsCorrectValue(String paymentAmount, String expectedEarning) {
            // given
            BigDecimal amount = new BigDecimal(paymentAmount);
            BigDecimal expected = new BigDecimal(expectedEarning);

            // when
            BigDecimal result = mileageService.calculateEarningAmount(amount);

            // then
            assertThat(result).isEqualByComparingTo(expected);
        }
    }

    @Nested
    @DisplayName("최종 결제 금액 계산 테스트")
    class CalculateFinalAmountTest {

        @Test
        @DisplayName("정상적인 마일리지 할인 적용")
        void calculateFinalAmount_ValidDiscount_ReturnsCorrectValue() {
            // given
            BigDecimal originalAmount = new BigDecimal("20000");
            BigDecimal usedMileage = new BigDecimal("5000");

            // when
            BigDecimal result = mileageService.calculateFinalAmount(originalAmount, usedMileage);

            // then
            assertThat(result).isEqualByComparingTo(new BigDecimal("15000"));
        }

        @Test
        @DisplayName("마일리지 할인이 결제 금액보다 큰 경우 - 0원 반환")
        void calculateFinalAmount_DiscountExceedsAmount_ReturnsZero() {
            // given
            BigDecimal originalAmount = new BigDecimal("5000");
            BigDecimal usedMileage = new BigDecimal("10000");

            // when
            BigDecimal result = mileageService.calculateFinalAmount(originalAmount, usedMileage);

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("실제 시나리오 통합 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("50,000원 결제, 12,000포인트 보유, 5,000포인트 사용")
        void realScenario_NormalPayment() {
            // given
            BigDecimal paymentAmount = new BigDecimal("50000");
            BigDecimal availableMileage = new BigDecimal("12000");
            BigDecimal requestedMileage = new BigDecimal("5000");

            // when & then
            boolean isValid = mileageService.validateMileageUsage(requestedMileage, availableMileage, paymentAmount);
            assertThat(isValid).isTrue();

            BigDecimal maxUsable = mileageService.calculateMaxUsableAmount(paymentAmount);
            assertThat(maxUsable).isEqualByComparingTo(new BigDecimal("15000"));

            BigDecimal finalAmount = mileageService.calculateFinalAmount(paymentAmount, requestedMileage);
            assertThat(finalAmount).isEqualByComparingTo(new BigDecimal("45000"));

            BigDecimal expectedEarning = mileageService.calculateEarningAmount(finalAmount);
            assertThat(expectedEarning).isEqualByComparingTo(new BigDecimal("450"));
        }
    }
} 