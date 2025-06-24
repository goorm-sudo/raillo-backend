package com.sudo.railo.payment.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudo.railo.payment.application.dto.request.PaymentCalculationRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.application.service.PaymentCalculationService;
import com.sudo.railo.payment.presentation.controller.PaymentCalculationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("결제 계산 컨트롤러 테스트")
class PaymentCalculationControllerTest {
    
    @Mock
    private PaymentCalculationService paymentCalculationService;
    
    @InjectMocks
    private PaymentCalculationController paymentCalculationController;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("결제 계산 API 성공 테스트")
    void calculatePayment_Success() throws Exception {
        // Given
        mockMvc = MockMvcBuilders.standaloneSetup(paymentCalculationController).build();
        
        PaymentCalculationRequest request = PaymentCalculationRequest.builder()
            .externalOrderId("ORDER_001")
            .userId("USER_001")
            .originalAmount(BigDecimal.valueOf(50000))
            .requestedPromotions(Collections.emptyList())
            .build();
        
        String calculationId = UUID.randomUUID().toString();
        PaymentCalculationResponse response = PaymentCalculationResponse.builder()
            .calculationId(calculationId)
            .externalOrderId("ORDER_001")
            .originalAmount(BigDecimal.valueOf(50000))
            .finalPayableAmount(BigDecimal.valueOf(50000))
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .appliedPromotions(Collections.emptyList())
            .validationErrors(Collections.emptyList())
            .build();
        
        when(paymentCalculationService.calculatePayment(any(PaymentCalculationRequest.class)))
            .thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/payments/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.calculationId").value(calculationId))
            .andExpect(jsonPath("$.externalOrderId").value("ORDER_001"))
            .andExpect(jsonPath("$.originalAmount").value(50000))
            .andExpect(jsonPath("$.finalPayableAmount").value(50000));
    }
    
    @Test
    @DisplayName("결제 계산 조회 API 성공 테스트")
    void getCalculation_Success() throws Exception {
        // Given
        mockMvc = MockMvcBuilders.standaloneSetup(paymentCalculationController).build();
        
        String calculationId = UUID.randomUUID().toString();
        PaymentCalculationResponse response = PaymentCalculationResponse.builder()
            .calculationId(calculationId)
            .externalOrderId("ORDER_001")
            .originalAmount(BigDecimal.valueOf(50000))
            .finalPayableAmount(BigDecimal.valueOf(50000))
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .appliedPromotions(Collections.emptyList())
            .validationErrors(Collections.emptyList())
            .build();
        
        when(paymentCalculationService.getCalculation(calculationId))
            .thenReturn(response);
        
        // When & Then
        mockMvc.perform(get("/api/v1/payments/calculations/{calculationId}", calculationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.calculationId").value(calculationId))
            .andExpect(jsonPath("$.externalOrderId").value("ORDER_001"));
    }
    
    @Test
    @DisplayName("잘못된 요청 데이터 검증 테스트")
    void calculatePayment_ValidationError() throws Exception {
        // Given
        mockMvc = MockMvcBuilders.standaloneSetup(paymentCalculationController).build();
        
        PaymentCalculationRequest invalidRequest = PaymentCalculationRequest.builder()
            .externalOrderId("")  // 빈 문자열
            .userId("USER_001")
            .originalAmount(BigDecimal.valueOf(-1000))  // 음수
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/payments/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
} 