package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.application.dto.response.PaymentExecuteResponse;
import com.sudo.railo.payment.application.service.PaymentService;
import com.sudo.railo.payment.application.service.PaymentConfirmationService;
import com.sudo.railo.payment.interfaces.dto.request.PaymentConfirmRequest;
import com.sudo.railo.payment.interfaces.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * 결제 실행 컨트롤러
 * 신용카드, 계좌이체 등 직접 결제 처리
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentExecuteController {
    
    private final PaymentService paymentService;
    private final PaymentConfirmationService paymentConfirmationService;
    
    /**
     * 결제 실행
     * POST /api/v1/payments/execute
     */
    @PostMapping("/execute")
    public ResponseEntity<PaymentExecuteResponse> executePayment(
            @RequestBody @Valid PaymentExecuteRequest request) {
        
        log.info("결제 실행 요청: calculationId={}, paymentMethod={}", 
                request.getCalculationId(), request.getPaymentMethod().getType());
        
        try {
            PaymentExecuteResponse response = paymentService.executePayment(request);
            
            log.info("결제 실행 완료: paymentId={}, status={}", 
                    response.getPaymentId(), response.getPaymentStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("결제 실행 중 오류 발생", e);
            throw e;
        }
    }
    
    /**
     * PG 결제 확인 (새로운 보안 강화 API)
     * POST /api/v1/payments/confirm
     * 
     * 프론트엔드 플로우:
     * 1. /calculate API로 계산 세션 생성 → calculationId 받음
     * 2. PG 결제창에서 결제 진행 → PG 승인번호 받음
     * 3. 이 API로 최종 확인 → 서버에서 PG 검증 후 결제 완료
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @RequestBody @Valid PaymentConfirmRequest request) {
        
        log.info("PG 결제 확인 요청: calculationId={}, pgAuthNumber={}", 
                request.getCalculationId(), request.getPgAuthNumber());
        
        try {
            PaymentResponse response = paymentConfirmationService.confirmPayment(request);
            
            log.info("PG 결제 확인 완료: paymentId={}, status={}", 
                    response.getPaymentId(), response.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("PG 결제 확인 중 오류 발생", e);
            throw e;
        }
    }
    
    /**
     * 결제 조회
     * GET /api/v1/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentExecuteResponse> getPayment(@PathVariable Long paymentId) {
        
        log.debug("결제 조회 요청: paymentId={}", paymentId);
        
        PaymentExecuteResponse response = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(response);
    }
}