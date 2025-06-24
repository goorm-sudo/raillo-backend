package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.request.PaymentCalculationRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.domain.entity.PaymentCalculation;
import com.sudo.railo.payment.domain.entity.CalculationStatus;
import com.sudo.railo.payment.domain.repository.PaymentCalculationRepository;
import com.sudo.railo.payment.domain.service.PaymentValidationService;
import com.sudo.railo.payment.domain.service.MileageService;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.application.event.PaymentEventPublisher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PaymentCalculationService 마일리지 통합 테스트
 * 
 * 테스트 범위:
 * - 마일리지 사용 검증 통합
 * - 마일리지 정보 생성 및 응답
 * - 마일리지 프로모션 적용
 * - 최종 금액 계산 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCalculationService 마일리지 통합 테스트")
class PaymentCalculationServiceMileageTest {

    @Mock
    private PaymentCalculationRepository calculationRepository;
    
    @Mock
    private PaymentValidationService validationService;
    
    @Mock
    private PaymentEventPublisher eventPublisher;
    
    private MileageService mileageService;
    private PaymentCalculationService paymentCalculationService;

    @BeforeEach
    void setUp() {
        mileageService = new MileageService();
        
        paymentCalculationService = new PaymentCalculationService(
            calculationRepository,
            validationService,
            mileageService,
            eventPublisher
        );
    }

    @Nested
    @DisplayName("마일리지 사용 결제 계산 테스트")
    class MileagePaymentCalculationTest {

        @Test
        @DisplayName("정상적인 마일리지 사용 - 계산 성공")
        void calculatePayment_WithValidMileage_Success() {
            // given
            PaymentCalculationRequest request = PaymentCalculationRequest.builder()
                .externalOrderId("ORDER-001")
                .userId("USER-001")
                .originalAmount(new BigDecimal("50000"))
                .mileageToUse(new BigDecimal("5000"))
                .availableMileage(new BigDecimal("10000"))
                .build();

            PaymentCalculation savedCalculation = PaymentCalculation.builder()
                .calculationId("CALC-001")
                .externalOrderId("ORDER-001")
                .userIdExternal("USER-001")
                .originalAmount(new BigDecimal("50000"))
                .finalAmount(new BigDecimal("45000"))
                .status(CalculationStatus.CALCULATED)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

            // mocking
            doNothing().when(validationService).validateCalculationRequest(request);
            when(calculationRepository.save(any(PaymentCalculation.class))).thenReturn(savedCalculation);
            doNothing().when(eventPublisher).publishCalculationEvent(anyString(), anyString(), anyString());

            // when
            PaymentCalculationResponse response = paymentCalculationService.calculatePayment(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getOriginalAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(response.getFinalPayableAmount()).isEqualByComparingTo(new BigDecimal("45000"));
            
            // 마일리지 정보 검증
            PaymentCalculationResponse.MileageInfo mileageInfo = response.getMileageInfo();
            assertThat(mileageInfo).isNotNull();
            assertThat(mileageInfo.getUsedMileage()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(mileageInfo.getMileageDiscount()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(mileageInfo.getAvailableMileage()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(mileageInfo.getMaxUsableMileage()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(mileageInfo.getExpectedEarning()).isEqualByComparingTo(new BigDecimal("450"));
            assertThat(mileageInfo.getUsageRateDisplay()).isEqualTo("10.0%");

            // 프로모션 적용 검증
            assertThat(response.getAppliedPromotions()).hasSize(1);
            PaymentCalculationResponse.AppliedPromotion promotion = response.getAppliedPromotions().get(0);
            assertThat(promotion.getType()).isEqualTo("MILEAGE");
            assertThat(promotion.getPointsUsed()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(promotion.getAmountDeducted()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(promotion.getStatus()).isEqualTo("APPLIED");

            // 저장 및 이벤트 발행 검증
            verify(calculationRepository).save(any(PaymentCalculation.class));
            verify(eventPublisher).publishCalculationEvent(anyString(), eq("ORDER-001"), eq("USER-001"));
        }

        @Test
        @DisplayName("마일리지 사용하지 않는 경우 - 계산 성공")
        void calculatePayment_WithoutMileage_Success() {
            // given
            PaymentCalculationRequest request = PaymentCalculationRequest.builder()
                .externalOrderId("ORDER-002")
                .userId("USER-002")
                .originalAmount(new BigDecimal("30000"))
                .mileageToUse(BigDecimal.ZERO)
                .availableMileage(new BigDecimal("5000"))
                .build();

            PaymentCalculation savedCalculation = PaymentCalculation.builder()
                .calculationId("CALC-002")
                .externalOrderId("ORDER-002")
                .userIdExternal("USER-002")
                .originalAmount(new BigDecimal("30000"))
                .finalAmount(new BigDecimal("30000"))
                .status(CalculationStatus.CALCULATED)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

            // mocking
            doNothing().when(validationService).validateCalculationRequest(request);
            when(calculationRepository.save(any(PaymentCalculation.class))).thenReturn(savedCalculation);
            doNothing().when(eventPublisher).publishCalculationEvent(anyString(), anyString(), anyString());

            // when
            PaymentCalculationResponse response = paymentCalculationService.calculatePayment(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getOriginalAmount()).isEqualByComparingTo(new BigDecimal("30000"));
            assertThat(response.getFinalPayableAmount()).isEqualByComparingTo(new BigDecimal("30000"));
            
            // 마일리지 정보 검증
            PaymentCalculationResponse.MileageInfo mileageInfo = response.getMileageInfo();
            assertThat(mileageInfo).isNotNull();
            assertThat(mileageInfo.getUsedMileage()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mileageInfo.getMileageDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mileageInfo.getAvailableMileage()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(mileageInfo.getMaxUsableMileage()).isEqualByComparingTo(new BigDecimal("9000"));
            assertThat(mileageInfo.getExpectedEarning()).isEqualByComparingTo(new BigDecimal("300"));
            assertThat(mileageInfo.getUsageRateDisplay()).isEqualTo("0.0%");

            // 프로모션 적용되지 않음 검증
            assertThat(response.getAppliedPromotions()).isEmpty();
        }

        @Test
        @DisplayName("마일리지 검증 실패 - 예외 발생")
        void calculatePayment_InvalidMileage_ThrowsException() {
            // given
            PaymentCalculationRequest request = PaymentCalculationRequest.builder()
                .externalOrderId("ORDER-003")
                .userId("USER-003")
                .originalAmount(new BigDecimal("10000"))
                .mileageToUse(new BigDecimal("5000"))
                .availableMileage(new BigDecimal("10000"))
                .build();

            // mocking
            doNothing().when(validationService).validateCalculationRequest(request);

            // when & then
            assertThatThrownBy(() -> paymentCalculationService.calculatePayment(request))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessage("마일리지 사용 조건을 만족하지 않습니다");

            // 저장 및 이벤트 발행되지 않음 검증
            verify(calculationRepository, never()).save(any());
            verify(eventPublisher, never()).publishCalculationEvent(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("보유 마일리지 부족 - 예외 발생")
        void calculatePayment_InsufficientMileage_ThrowsException() {
            // given
            PaymentCalculationRequest request = PaymentCalculationRequest.builder()
                .externalOrderId("ORDER-004")
                .userId("USER-004")
                .originalAmount(new BigDecimal("50000"))
                .mileageToUse(new BigDecimal("8000"))     // 8,000포인트 요청
                .availableMileage(new BigDecimal("5000")) // 5,000포인트만 보유
                .build();

            // mocking
            doNothing().when(validationService).validateCalculationRequest(request);

            // when & then
            assertThatThrownBy(() -> paymentCalculationService.calculatePayment(request))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessage("마일리지 사용 조건을 만족하지 않습니다");
        }
    }

    @Nested
    @DisplayName("마일리지 정보 생성 테스트")
    class MileageInfoGenerationTest {

        @Test
        @DisplayName("마일리지 정보 정확성 검증")
        void buildMileageInfo_CalculatesCorrectValues() {
            // given
            PaymentCalculationRequest request = PaymentCalculationRequest.builder()
                .externalOrderId("ORDER-005")
                .userId("USER-005")
                .originalAmount(new BigDecimal("100000"))  // 100,000원
                .mileageToUse(new BigDecimal("15000"))     // 15,000포인트 사용
                .availableMileage(new BigDecimal("20000")) // 20,000포인트 보유
                .build();

            PaymentCalculation savedCalculation = PaymentCalculation.builder()
                .calculationId("CALC-005")
                .externalOrderId("ORDER-005")
                .userIdExternal("USER-005")
                .originalAmount(new BigDecimal("100000"))
                .finalAmount(new BigDecimal("85000"))
                .status(CalculationStatus.CALCULATED)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

            // mocking
            doNothing().when(validationService).validateCalculationRequest(request);
            when(calculationRepository.save(any(PaymentCalculation.class))).thenReturn(savedCalculation);
            doNothing().when(eventPublisher).publishCalculationEvent(anyString(), anyString(), anyString());

            // when
            PaymentCalculationResponse response = paymentCalculationService.calculatePayment(request);

            // then
            PaymentCalculationResponse.MileageInfo mileageInfo = response.getMileageInfo();
            assertThat(mileageInfo.getUsedMileage()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(mileageInfo.getMileageDiscount()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(mileageInfo.getAvailableMileage()).isEqualByComparingTo(new BigDecimal("20000"));
            assertThat(mileageInfo.getMaxUsableMileage()).isEqualByComparingTo(new BigDecimal("30000")); // 100,000 * 30%
            assertThat(mileageInfo.getRecommendedMileage()).isEqualByComparingTo(new BigDecimal("20000")); // 보유한 전체
            assertThat(mileageInfo.getExpectedEarning()).isEqualByComparingTo(new BigDecimal("850")); // 85,000 * 1%
            assertThat(mileageInfo.getUsageRate()).isEqualByComparingTo(new BigDecimal("0.1500")); // 15%
            assertThat(mileageInfo.getUsageRateDisplay()).isEqualTo("15.0%");
        }

        @Test
        @DisplayName("권장 마일리지가 최대 사용량보다 적은 경우")
        void buildMileageInfo_RecommendedLessThanMax() {
            // given
            PaymentCalculationRequest request = PaymentCalculationRequest.builder()
                .externalOrderId("ORDER-006")
                .userId("USER-006")
                .originalAmount(new BigDecimal("100000"))  // 100,000원 (최대 30,000포인트 사용 가능)
                .mileageToUse(new BigDecimal("8000"))      // 8,000포인트 사용
                .availableMileage(new BigDecimal("12000")) // 12,000포인트 보유
                .build();

            PaymentCalculation savedCalculation = PaymentCalculation.builder()
                .calculationId("CALC-006")
                .externalOrderId("ORDER-006")
                .userIdExternal("USER-006")
                .originalAmount(new BigDecimal("100000"))
                .finalAmount(new BigDecimal("92000"))
                .status(CalculationStatus.CALCULATED)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

            // mocking
            doNothing().when(validationService).validateCalculationRequest(request);
            when(calculationRepository.save(any(PaymentCalculation.class))).thenReturn(savedCalculation);
            doNothing().when(eventPublisher).publishCalculationEvent(anyString(), anyString(), anyString());

            // when
            PaymentCalculationResponse response = paymentCalculationService.calculatePayment(request);

            // then
            PaymentCalculationResponse.MileageInfo mileageInfo = response.getMileageInfo();
            assertThat(mileageInfo.getMaxUsableMileage()).isEqualByComparingTo(new BigDecimal("30000"));
            assertThat(mileageInfo.getRecommendedMileage()).isEqualByComparingTo(new BigDecimal("12000")); // 보유한 만큼만
        }
    }

    @Nested
    @DisplayName("계산 세션 조회 테스트")
    class CalculationRetrievalTest {

        @Test
        @DisplayName("정상적인 계산 세션 조회")
        void getCalculation_ValidSession_ReturnsResponse() {
            // given
            String calculationId = "CALC-007";
            PaymentCalculation calculation = PaymentCalculation.builder()
                .calculationId(calculationId)
                .externalOrderId("ORDER-007")
                .userIdExternal("USER-007")
                .originalAmount(new BigDecimal("25000"))
                .finalAmount(new BigDecimal("22000"))
                .status(CalculationStatus.CALCULATED)
                .expiresAt(LocalDateTime.now().plusMinutes(15)) // 아직 만료되지 않음
                .promotionSnapshot("[]")
                .build();

            // mocking
            when(calculationRepository.findById(calculationId)).thenReturn(Optional.of(calculation));

            // when
            PaymentCalculationResponse response = paymentCalculationService.getCalculation(calculationId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCalculationId()).isEqualTo(calculationId);
            assertThat(response.getExternalOrderId()).isEqualTo("ORDER-007");
            assertThat(response.getOriginalAmount()).isEqualByComparingTo(new BigDecimal("25000"));
            assertThat(response.getFinalPayableAmount()).isEqualByComparingTo(new BigDecimal("22000"));
        }

        @Test
        @DisplayName("존재하지 않는 계산 세션 - 예외 발생")
        void getCalculation_NotFound_ThrowsException() {
            // given
            String calculationId = "INVALID-CALC";

            // mocking
            when(calculationRepository.findById(calculationId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentCalculationService.getCalculation(calculationId))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessage("계산 세션을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("만료된 계산 세션 - 예외 발생")
        void getCalculation_Expired_ThrowsException() {
            // given
            String calculationId = "EXPIRED-CALC";
            PaymentCalculation calculation = PaymentCalculation.builder()
                .calculationId(calculationId)
                .externalOrderId("ORDER-008")
                .userIdExternal("USER-008")
                .originalAmount(new BigDecimal("15000"))
                .finalAmount(new BigDecimal("15000"))
                .status(CalculationStatus.CALCULATED)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // 5분 전에 만료됨
                .promotionSnapshot("[]")
                .build();

            // mocking
            when(calculationRepository.findById(calculationId)).thenReturn(Optional.of(calculation));

            // when & then
            assertThatThrownBy(() -> paymentCalculationService.getCalculation(calculationId))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessage("계산 세션이 만료되었습니다");
        }
    }

    @Nested
    @DisplayName("실제 시나리오 통합 테스트")
    class BusinessScenarioTest {

        @Test
        @DisplayName("기차표 예약 결제 (마일리지 사용)")
        void realScenario_TrainTicketWithMileage() {
            // given
            PaymentCalculationRequest request = PaymentCalculationRequest.builder()
                .externalOrderId("TRAIN-TICKET-001")
                .userId("MEMBER-12345")
                .originalAmount(new BigDecimal("80000"))
                .mileageToUse(new BigDecimal("15000"))
                .availableMileage(new BigDecimal("25000"))
                .build();

            PaymentCalculation savedCalculation = PaymentCalculation.builder()
                .calculationId("CALC-TRAIN-001")
                .externalOrderId("TRAIN-TICKET-001")
                .userIdExternal("MEMBER-12345")
                .originalAmount(new BigDecimal("80000"))
                .finalAmount(new BigDecimal("65000"))
                .status(CalculationStatus.CALCULATED)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

            // mocking
            doNothing().when(validationService).validateCalculationRequest(request);
            when(calculationRepository.save(any(PaymentCalculation.class))).thenReturn(savedCalculation);
            doNothing().when(eventPublisher).publishCalculationEvent(anyString(), anyString(), anyString());

            // when
            PaymentCalculationResponse response = paymentCalculationService.calculatePayment(request);

            // then
            assertThat(response.getOriginalAmount()).isEqualByComparingTo(new BigDecimal("80000"));
            assertThat(response.getFinalPayableAmount()).isEqualByComparingTo(new BigDecimal("65000"));

            PaymentCalculationResponse.MileageInfo mileageInfo = response.getMileageInfo();
            assertThat(mileageInfo.getUsedMileage()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(mileageInfo.getMileageDiscount()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(mileageInfo.getExpectedEarning()).isEqualByComparingTo(new BigDecimal("650"));
            assertThat(mileageInfo.getUsageRateDisplay()).isEqualTo("18.8%");

            assertThat(response.getAppliedPromotions()).hasSize(1);
            PaymentCalculationResponse.AppliedPromotion promotion = response.getAppliedPromotions().get(0);
            assertThat(promotion.getType()).isEqualTo("MILEAGE");
            assertThat(promotion.getDescription()).isEqualTo("마일리지 15000포인트 사용");
        }
    }
} 