package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.request.PaymentCalculationRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.application.service.PaymentCalculationService;
import com.sudo.railo.payment.exception.PaymentException;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentCalculationController {
    
    private final PaymentCalculationService paymentCalculationService;
    
    @PostMapping("/calculate")
    public ResponseEntity<PaymentCalculationResponse> calculatePayment(
            @RequestBody @Valid PaymentCalculationRequest request) {
        
        log.info("결제 계산 요청: orderId={}, userId={}, amount={}", 
            request.getExternalOrderId(), request.getUserId(), request.getOriginalAmount());
        
        try {
            PaymentCalculationResponse response = paymentCalculationService.calculatePayment(request);
            
            log.info("결제 계산 완료: calculationId={}, finalAmount={}", 
                response.getCalculationId(), response.getFinalPayableAmount());
            
            return ResponseEntity.ok(response);
            
        } catch (PaymentValidationException e) {
            log.warn("결제 계산 검증 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("결제 계산 중 오류 발생", e);
            throw new PaymentException("결제 계산 처리 중 오류가 발생했습니다", e);
        }
    }
    
    @GetMapping("/calculations/{calculationId}")
    public ResponseEntity<PaymentCalculationResponse> getCalculation(
            @PathVariable String calculationId) {
        
        PaymentCalculationResponse response = paymentCalculationService.getCalculation(calculationId);
        return ResponseEntity.ok(response);
    }
}