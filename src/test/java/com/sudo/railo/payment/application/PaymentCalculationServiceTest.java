package com.sudo.railo.payment.application;

import com.sudo.railo.payment.application.dto.request.PaymentCalculationRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.application.service.PaymentCalculationService;
import com.sudo.railo.payment.domain.entity.PaymentCalculation;
import com.sudo.railo.payment.domain.repository.PaymentCalculationRepository;
import com.sudo.railo.payment.domain.service.PaymentValidationService;
import com.sudo.railo.payment.domain.service.MileageService;
import com.sudo.railo.payment.application.event.PaymentEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("결제 계산 서비스 테스트")
class PaymentCalculationServiceTest {
    
    @Mock
    private PaymentCalculationRepository calculationRepository;
    
    @Mock
    private PaymentValidationService validationService;
    
    @Mock
    private PaymentEventPublisher eventPublisher;
    
    private MileageService mileageService;
    private PaymentCalculationService paymentCalculationService;
    
    private PaymentCalculationRequest testRequest;
    
    @BeforeEach
    void setUp() {
        // 실제 MileageService 사용
        mileageService = new MileageService();
        
        paymentCalculationService = new PaymentCalculationService(
            calculationRepository,
            validationService,
            mileageService,
            eventPublisher
        );
        
        testRequest = PaymentCalculationRequest.builder()
            .externalOrderId("ORDER_001")
            .userId("USER_001")
            .originalAmount(BigDecimal.valueOf(50000))
            .mileageToUse(BigDecimal.ZERO)
            .availableMileage(BigDecimal.ZERO)
            .requestedPromotions(Collections.emptyList())
            .build();
    }
    
    @Test
    @DisplayName("결제 계산 성공 테스트")
    void calculatePayment_Success() {
        // Given
        when(calculationRepository.save(any(PaymentCalculation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PaymentCalculationResponse response = paymentCalculationService.calculatePayment(testRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getExternalOrderId()).isEqualTo("ORDER_001");
        assertThat(response.getOriginalAmount()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(response.getFinalPayableAmount()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(response.getCalculationId()).isNotNull();
        assertThat(response.getMileageInfo()).isNotNull();
        assertThat(response.getMileageInfo().getUsedMileage()).isEqualByComparingTo(BigDecimal.ZERO);
        
        verify(validationService).validateCalculationRequest(testRequest);
        verify(calculationRepository).save(any(PaymentCalculation.class));
        verify(eventPublisher).publishCalculationEvent(anyString(), eq("ORDER_001"), eq("USER_001"));
    }
    
    @Test
    @DisplayName("마일리지 사용 계산 테스트")
    void calculatePayment_WithMileage() {
        // Given
        testRequest = PaymentCalculationRequest.builder()
            .externalOrderId("ORDER_002")
            .userId("USER_002")
            .originalAmount(BigDecimal.valueOf(50000))
            .mileageToUse(BigDecimal.valueOf(5000))
            .availableMileage(BigDecimal.valueOf(10000))
            .requestedPromotions(Collections.emptyList())
            .build();
        
        when(calculationRepository.save(any(PaymentCalculation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PaymentCalculationResponse response = paymentCalculationService.calculatePayment(testRequest);
        
        // Then
        assertThat(response.getFinalPayableAmount()).isEqualByComparingTo(BigDecimal.valueOf(45000));
        assertThat(response.getMileageInfo().getUsedMileage()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(response.getMileageInfo().getMileageDiscount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(response.getAppliedPromotions()).hasSize(1);
        assertThat(response.getAppliedPromotions().get(0).getType()).isEqualTo("MILEAGE");
        assertThat(response.getAppliedPromotions().get(0).getPointsUsed()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }
} 