package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.application.dto.response.PaymentExecuteResponse;
import com.sudo.railo.payment.application.service.PaymentService;
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
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/execute")
    public ResponseEntity<PaymentExecuteResponse> executePayment(
            @RequestBody @Valid PaymentExecuteRequest request) {
        
        log.info("결제 실행 요청: calculationId={}, idempotencyKey={}", 
            request.getCalculationId(), request.getIdempotencyKey());
        
        try {
            PaymentExecuteResponse response = paymentService.executePayment(request);
            
            log.info("결제 실행 완료: paymentId={}, status={}, amount={}", 
                response.getPaymentId(), response.getPaymentStatus(), response.getAmountPaid());
            
            return ResponseEntity.ok(response);
            
        } catch (PaymentValidationException e) {
            log.warn("결제 실행 검증 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("결제 실행 중 오류 발생", e);
            throw new PaymentException("결제 처리 중 오류가 발생했습니다", e);
        }
    }
    
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentExecuteResponse> getPayment(
            @PathVariable Long paymentId) {
        
        PaymentExecuteResponse response = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(response);
    }
} 